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
        blockUpdate.verifyFinalizedHeader();
        var bmvProperties = getProperties();

        var currentSyncCommittee = SyncCommittee.deserialize(bmvProperties.getCurrentSyncCommittee());
        logger.println("processBlockUpdate, ", "verify syncAggregate");
        if (!blockUpdate.verifySyncAggregate(currentSyncCommittee.getBlsPublicKeys(), bmvProperties.getGenesisValidatorsHash(), blockUpdate.getSignatureSlot()))
            throw BMVException.unknown("invalid signature");

        var nextSyncCommittee = blockUpdate.getNextSyncCommittee();
        if (nextSyncCommittee != null) {
            logger.println("processBlockUpdate, ", "verify nextSyncCommittee aggregatedKey : " + StringUtil.toString(nextSyncCommittee.getAggregatePubKey()));
            blockUpdate.verifyNextSyncCommittee();
        }

        applyBlockUpdate(blockUpdate, bmvProperties);
    }

    private void applyBlockUpdate(BlockUpdate blockUpdate, BMVProperties bmvProperties) {
        var nextSyncCommittee = bmvProperties.getNextSyncCommittee();
        var lightClientHeader = bmvProperties.getFinalizedHeader();
        var beaconBlockHeader = lightClientHeader.getBeacon();
        var storeSlot = beaconBlockHeader.getSlot();
        var updateFinalizedSlot = LightClientHeader.deserialize(blockUpdate.getFinalizedHeader()).getBeacon().getSlot();
        var storePeriod = Utils.computeSyncCommitteePeriod(Utils.computeEpoch(storeSlot));
        var updateFinalizedPeriod = Utils.computeSyncCommitteePeriod(Utils.computeEpoch(updateFinalizedSlot));
        if (nextSyncCommittee == null) {
            logger.println("applyBlockUpdate, ", " assert period. finalizedPeriod : ", updateFinalizedPeriod, ", storedPeriod : ", storePeriod);
            if (updateFinalizedPeriod.compareTo(storePeriod) != 0) throw BMVException.unknown("invalid update period");
            bmvProperties.setNextSyncCommittee(blockUpdate.getNextSyncCommittee().toBytes());
        } else if (updateFinalizedPeriod.compareTo(storePeriod.add(BigInteger.ONE)) == 0) {
            logger.println("applyBlockUpdate, ", "set current/next sync committee");
            bmvProperties.setCurrentSyncCommittee(bmvProperties.getNextSyncCommittee());
            bmvProperties.setNextSyncCommittee(blockUpdate.getNextSyncCommittee().toBytes());
        }
        if (updateFinalizedPeriod.compareTo(storeSlot) > 0) {
            logger.println("applyBlockUpdate, ", "set current/next sync committee");
            bmvProperties.setFinalizedHeader(LightClientHeader.deserialize(blockUpdate.getFinalizedHeader()));
        }
        propertiesDB.set(bmvProperties);
    }

    private void processBlockProof(BlockProof blockProof) {
        logger.println("processBlockProof, ", blockProof);
        var bmvProperties = getProperties();
        var finalizedHeader = bmvProperties.getFinalizedHeader();
        var attestingHeader = blockProof.getLightClientHeader();
        var attestingBeacon = attestingHeader.getBeacon();
        var finalizedSlot = finalizedHeader.getBeacon().getSlot();
        var blockProofSlot = attestingBeacon.getSlot();
        if (finalizedSlot.compareTo(blockProofSlot) < 0)
            throw BMVException.invalidBlockProofSlot(blockProofSlot.toString());
        var hashTree = attestingBeacon.getHashTreeRoot();
        var proofLeaf = blockProof.getProof().getLeaf();
        if (!Arrays.equals(proofLeaf, hashTree))
            throw BMVException.unknown("invalid hashTree");
        SszUtils.verify(finalizedHeader.getBeacon().getStateRoot(), blockProof.getProof());
    }

    private byte[][] processMessageProof(MessageProof messageProof, BlockProof blockProof, String _bmc) {
        logger.println("processMessageProof, ", "messageProof : ", messageProof, ", BlockProof", blockProof);
        var bmvProperties = getProperties();
        var lightClientHeader = blockProof.getLightClientHeader();
        var beaconBlockHeader = lightClientHeader.getBeacon();
        var stateRoot = beaconBlockHeader.getStateRoot();
        var receiptRootProof = messageProof.getReceiptRootProof();
        logger.println("processMessageProof, ", "stateRoot", stateRoot, ", receiptRootProof : ", receiptRootProof);
        SszUtils.verify(stateRoot, receiptRootProof);
        var receiptsRoot = receiptRootProof.getLeaf();
        logger.println("processMessageProof, ", "receiptsRoot : ", receiptsRoot);
        var messageList = new ArrayList<byte[]>();
        for (ReceiptProof rp : messageProof.getReceiptProofs()) {
            logger.println("processMessageProof, ", "mpt prove", ", receiptProof key : ", rp.getKey());
            var value = MerklePatriciaTree.prove(receiptsRoot, rp.getKey(), rp.getProofs());
            var receipt = Receipt.fromBytes(value);
            logger.println("processMessageProof, ", "receipt : ", receipt);
            for (Log log : receipt.getLogs()) {
                var topics = log.getTopics();
                var topic = topics[0];
                var nextHash = topics[1];
                var bmcBtpAddress = BTPAddress.valueOf(_bmc);
                if (Arrays.equals(nextHash, Context.hash("keccak-256", bmcBtpAddress.toString().getBytes())) && Arrays.equals(topic, eventSignatureTopic)) {
                    logger.println("processMessageProof, ", "add message. log : ", log);
                    var msg = log.getData();
                    messageList.add(msg);
                }
            }
        }
        var cnt = messageList.size();
        var messages = new byte[cnt][];
        if (cnt != 0) {
            for (int i = 0; i < cnt; i++)
                messages[i] = messageList.get(i);
            bmvProperties.setLastMsgSeq(bmvProperties.getLastMsgSeq().add(BigInteger.valueOf(cnt)));
            bmvProperties.setLastMsgSlot(beaconBlockHeader.getSlot());
            propertiesDB.set(bmvProperties);
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
