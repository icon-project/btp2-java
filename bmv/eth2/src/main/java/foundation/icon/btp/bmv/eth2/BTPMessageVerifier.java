/*
 * Copyright 2023 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package foundation.icon.btp.bmv.eth2;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class BTPMessageVerifier implements BMV {
    private static final Logger logger = Logger.getLogger(BTPMessageVerifier.class);
    private final VarDB<BMVProperties> propertiesDB = Context.newVarDB("properties", BMVProperties.class);
    private final String eventSignature = "Message(string,uint256,bytes)";
    private final byte[] eventSignatureTopic = Context.hash("keccak-256", eventSignature.getBytes());

    public BTPMessageVerifier(String srcNetworkID, byte[] genesisValidatorsHash, byte[] syncCommittee, Address bmc, byte[] finalizedHeader) {
        var properties = getProperties();
        properties.setSrcNetworkID(srcNetworkID.getBytes());
        properties.setBmc(bmc);
        properties.setGenesisValidatorsHash(genesisValidatorsHash);
        properties.setCurrentSyncCommittee(syncCommittee);
        properties.setFinalizedHeader(LightClientHeader.deserialize(finalizedHeader));
        properties.setLastMsgSlot(BigInteger.ZERO);
        properties.setLastMsgSeq(BigInteger.ZERO);
        propertiesDB.set(properties);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
        logger.println("handleRelayMessage, msg : ", StringUtil.toString(_msg));
        BTPAddress curAddr = BTPAddress.valueOf(_bmc);
        BTPAddress prevAddr = BTPAddress.valueOf(_prev);
        checkAccessible(curAddr, prevAddr);
        var bmvProperties = getProperties();
        var lastSeq = bmvProperties.getLastMsgSeq();
        if (lastSeq.compareTo(_seq) != 0) throw BMVException.unknown("invalid sequence");
        RelayMessage relayMessages = RelayMessage.fromBytes(_msg);
        RelayMessage.TypePrefixedMessage[] typePrefixedMessages = relayMessages.getMessages();
        BlockProof blockProof = null;
        List<byte[]> msgList = new ArrayList<>();
        for (RelayMessage.TypePrefixedMessage message : typePrefixedMessages) {
            Object msg = message.getMessage();
            if (msg instanceof BlockUpdate) {
                logger.println("handleRelayMessage, blockUpdate : " + msg);
                processBlockUpdate((BlockUpdate) msg);
            } else if (msg instanceof BlockProof) {
                logger.println("handleRelayMessage, blockProof : " + msg);
                blockProof = (BlockProof) msg;
                processBlockProof(blockProof);
            } else if (msg instanceof MessageProof) {
                logger.println("handleRelayMessage, MessageProof : " + msg);
                var msgs = processMessageProof((MessageProof) msg, blockProof, _bmc);
                for(byte[] m : msgs) {
                    msgList.add(m);
                }
            }
        }
        var retSize = msgList.size();
        var ret = new byte[retSize][];
        if (retSize > 0) {
            for (int i = 0; i < retSize; i ++) {
                ret[i] = msgList.get(i);
            }
        }
        return ret;
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        var properties = getProperties();
        BMVStatus s = new BMVStatus();
        var finalizedHeaderSlot = properties.getFinalizedHeader().getBeacon().getSlot();
        s.setHeight(finalizedHeaderSlot.longValue());
        s.setExtra(new BMVStatusExtra(
                properties.getLastMsgSeq(),
                properties.getLastMsgSlot()).toBytes());
        return s;
    }

    BMVProperties getProperties() {
        return propertiesDB.getOrDefault(BMVProperties.DEFAULT);
    }

    private void processBlockUpdate(BlockUpdate blockUpdate) {
        var properties = getProperties();
        validateBlockUpdate(blockUpdate, properties);
        applyBlockUpdate(blockUpdate, properties);
    }

    private void validateBlockUpdate(BlockUpdate blockUpdate, BMVProperties properties) {
        var attestedBeacon = LightClientHeader.deserialize(blockUpdate.getAttestedHeader()).getBeacon();
        var finalizedBeacon = LightClientHeader.deserialize(blockUpdate.getFinalizedHeader()).getBeacon();
        var signatureSlot = blockUpdate.getSignatureSlot();
        var attestedSlot = attestedBeacon.getSlot();
        var finalizedSlot = finalizedBeacon.getSlot();
        if (signatureSlot.compareTo(attestedSlot) <= 0) throw BMVException.unknown("signature slot( + " + signatureSlot + ") must be after attested Slot(" + attestedSlot + ")");
        if (attestedSlot.compareTo(finalizedSlot) < 0) throw BMVException.unknown("attested slot (" + attestedSlot + ") must be after finalized slot(" + finalizedSlot + ")");

        var storedFinalizedBeacon = properties.getFinalizedHeader().getBeacon();
        var storedSlot = storedFinalizedBeacon.getSlot();
        var storedPeriod = Utils.computeSyncCommitteePeriod(storedSlot);
        var signaturePeriod = Utils.computeSyncCommitteePeriod(signatureSlot);

        if (properties.getNextSyncCommittee() != null)
            if (signaturePeriod.compareTo(storedPeriod) != 0 && signaturePeriod.compareTo(storedPeriod.add(BigInteger.ONE)) != 0)
                throw BMVException.notVerifiable(storedSlot.toString());
        else
            if (signaturePeriod.compareTo(storedPeriod) != 0)
                throw BMVException.notVerifiable(storedSlot.toString());

        blockUpdate.verifyFinalizedHeader();

        var nextSyncCommittee = blockUpdate.getNextSyncCommittee();
        if (nextSyncCommittee != null) {
            logger.println("validateBlockUpdate, ", "verify nextSyncCommittee aggregatedKey : " + StringUtil.toString(nextSyncCommittee.getAggregatePubKey()));
            blockUpdate.verifyNextSyncCommittee();
        }

        SyncCommittee syncCommittee;
        if (signaturePeriod.compareTo(storedPeriod) == 0)
            syncCommittee = SyncCommittee.deserialize(properties.getCurrentSyncCommittee());
        else
            syncCommittee = SyncCommittee.deserialize(properties.getNextSyncCommittee());
        logger.println("validateBlockUpdate, ", "verify syncAggregate");
        if (!blockUpdate.verifySyncAggregate(syncCommittee.getBlsPublicKeys(), properties.getGenesisValidatorsHash()))
            throw BMVException.unknown("invalid signature");
    }

    private void applyBlockUpdate(BlockUpdate blockUpdate, BMVProperties properties) {
        var storedNextSyncCommittee = properties.getNextSyncCommittee();
        var storedBeacon = properties.getFinalizedHeader().getBeacon();
        var storedSlot = storedBeacon.getSlot();
        var finalizedSlot = LightClientHeader.deserialize(blockUpdate.getFinalizedHeader()).getBeacon().getSlot();
        var storedPeriod = Utils.computeSyncCommitteePeriod(Utils.computeEpoch(storedSlot));
        var finalizedPeriod = Utils.computeSyncCommitteePeriod(Utils.computeEpoch(finalizedSlot));

        if (storedNextSyncCommittee == null) {
            logger.println("applyBlockUpdate, ", " assert period. finalizedPeriod : ", finalizedPeriod, ", storedPeriod : ", storedPeriod);
            if (finalizedPeriod.compareTo(storedPeriod) != 0) throw BMVException.unknown("invalid update period");
            properties.setNextSyncCommittee(blockUpdate.getNextSyncCommittee());
        } else if (finalizedPeriod.compareTo(storedPeriod.add(BigInteger.ONE)) == 0) {
            logger.println("applyBlockUpdate, ", "set current/next sync committee");
            properties.setCurrentSyncCommittee(properties.getNextSyncCommittee());
            properties.setNextSyncCommittee(blockUpdate.getNextSyncCommittee());
        }

        if (finalizedSlot.compareTo(storedSlot) > 0) {
            logger.println("applyBlockUpdate, ", "set finalized header");
            properties.setFinalizedHeader(LightClientHeader.deserialize(blockUpdate.getFinalizedHeader()));
        }
        propertiesDB.set(properties);
    }

    private void processBlockProof(BlockProof blockProof) {
        logger.println("processBlockProof, ", blockProof);
        var properties = getProperties();
        var storedBeacon = properties.getFinalizedHeader().getBeacon();
        var attestingBeacon = blockProof.getLightClientHeader().getBeacon();
        var storedFinalizedSlot = storedBeacon.getSlot();
        var blockProofSlot = attestingBeacon.getSlot();
        if (storedFinalizedSlot.compareTo(blockProofSlot) < 0)
            throw BMVException.unknown(blockProofSlot.toString());
        if (properties.getBpHeader() != null) {
            var storedBlockProofSlot = properties.getBpHeader().getBeacon().getSlot();
            if (blockProofSlot.compareTo(storedBlockProofSlot) < 0)
                throw BMVException.invalidBlockWitnessOld(blockProofSlot.toString());
        }
        var hashTree = attestingBeacon.getHashTreeRoot();
        var proofLeaf = blockProof.getProof().getLeaf();
        if (!Arrays.equals(proofLeaf, hashTree))
            throw BMVException.unknown("invalid hashTree");
        SszUtils.verify(storedBeacon.getStateRoot(), blockProof.getProof());
        properties.setBpHeader(blockProof.getLightClientHeader());
        propertiesDB.set(properties);
    }

    private byte[][] processMessageProof(MessageProof messageProof, BlockProof blockProof, String _bmc) {
        logger.println("processMessageProof, ", "messageProof : ", messageProof, ", BlockProof", blockProof);
        var properties = getProperties();
        var beaconBlockHeader = blockProof.getLightClientHeader().getBeacon();
        var stateRoot = beaconBlockHeader.getStateRoot();
        var receiptRootProof = messageProof.getReceiptRootProof();
        logger.println("processMessageProof, ", "stateRoot", stateRoot, ", receiptRootProof : ", receiptRootProof);
        SszUtils.verify(stateRoot, receiptRootProof);
        var receiptsRoot = receiptRootProof.getLeaf();
        logger.println("processMessageProof, ", "receiptsRoot : ", StringUtil.bytesToHex(receiptsRoot));
        var messageList = new ArrayList<byte[]>();
        for (ReceiptProof rp : messageProof.getReceiptProofs()) {
            logger.println("processMessageProof, ", "mpt prove", ", receiptProof key : ", StringUtil.bytesToHex(rp.getKey()));
            var value = MerklePatriciaTree.prove(receiptsRoot, rp.getKey(), rp.getProofs());
            var receipt = Receipt.fromBytes(value);
            logger.println("processMessageProof, ", "receipt : ", receipt);
            for (Log log : receipt.getLogs()) {
                var topics = log.getTopics();
                var signature = topics[0];
                if (!Arrays.equals(signature, eventSignatureTopic)) continue;
                var nextHash = topics[1];
                logger.println("processMessageProof, ", "topic : ", StringUtil.bytesToHex(signature), ", nextHash : ", StringUtil.bytesToHex(nextHash));
                if (Arrays.equals(nextHash, Context.hash("keccak-256", _bmc.getBytes()))) {
                    logger.println("processMessageProof, ", "add message. log : ", log);
                    var msg = log.getMessage();
                    messageList.add(msg);
                }
            }
        }
        var cnt = messageList.size();
        var messages = new byte[cnt][];
        if (cnt != 0) {
            for (int i = 0; i < cnt; i++)
                messages[i] = messageList.get(i);
            properties.setLastMsgSeq(properties.getLastMsgSeq().add(BigInteger.valueOf(cnt)));
            properties.setLastMsgSlot(beaconBlockHeader.getSlot());
            propertiesDB.set(properties);
            return messages;
        }
        return messages;
    }

    private void checkAccessible(BTPAddress curAddr, BTPAddress fromAddress) {
        BMVProperties properties = getProperties();
        if (!properties.getNetwork().equals(fromAddress.net())) {
            throw BMVException.unknown("invalid prev bmc");
        } else if (!Context.getCaller().equals(properties.getBmc())) {
            throw BMVException.unknown("invalid caller bmc");
        } else if (!Address.fromString(curAddr.account()).equals(properties.getBmc())) {
            throw BMVException.unknown("invalid current bmc");
        }
    }
}
