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
import score.annotation.Optional;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class BTPMessageVerifier implements BMV {
    private static final Logger logger = Logger.getLogger(BTPMessageVerifier.class);
    private final VarDB<BMVProperties> propertiesDB = Context.newVarDB("properties", BMVProperties.class);
    private final VarDB<MessageProofProperties> messageProofPropertiesDB = Context.newVarDB("messageProofProperties", MessageProofProperties.class);
    private final VarDB<SyncCommittee> currentSyncCommitteeDB = Context.newVarDB("currentSyncCommittee", SyncCommittee.class);
    private final VarDB<SyncCommittee> nextSyncCommitteeDB = Context.newVarDB("nextSyncCommittee", SyncCommittee.class);
    private final VarDB<LightClientHeader> finalizedHeaderDB = Context.newVarDB("finalizedHeader", LightClientHeader.class);
    private final VarDB<LightClientHeader> blockProofHeaderDB = Context.newVarDB("blockProofHeader", LightClientHeader.class);
    private final String eventSignature = "Message(string,uint256,bytes)";
    private final byte[] eventSignatureTopic = Context.hash("keccak-256", eventSignature.getBytes());

    public BTPMessageVerifier(
            @Optional String srcNetworkID,
            @Optional byte[] genesisValidatorsHash,
            @Optional byte[] syncCommittee,
            @Optional Address bmc,
            @Optional byte[] ethBmc,
            @Optional byte[] finalizedHeader,
            @Optional BigInteger seq
    ) {
        if (srcNetworkID == null && genesisValidatorsHash == null && syncCommittee == null && bmc == null && ethBmc == null && finalizedHeader == null && seq.signum() == 0) return;
        var properties = getProperties();
        var mpProperties = getMessageProofProperties();
        if (srcNetworkID != null) properties.setSrcNetworkID(srcNetworkID.getBytes());
        if (bmc != null) properties.setBmc(bmc);
        if (ethBmc != null) mpProperties.setEthBmc(ethBmc);
        if (genesisValidatorsHash != null) properties.setGenesisValidatorsHash(genesisValidatorsHash);
        if (syncCommittee != null) currentSyncCommitteeDB.set(SyncCommittee.deserialize(syncCommittee));
        if (finalizedHeader != null) finalizedHeaderDB.set(LightClientHeader.deserialize(finalizedHeader));
        if (seq.signum() == -1) throw BMVException.unknown("invalid seq. sequence must >= 0");
        var lastMsgSeq = mpProperties.getLastMsgSeq();
        if (lastMsgSeq == null || seq.signum() == 1) mpProperties.setLastMsgSeq(seq);
        if (mpProperties.getLastMsgSlot() == null) mpProperties.setLastMsgSlot(BigInteger.ZERO);
        propertiesDB.set(properties);
        messageProofPropertiesDB.set(mpProperties);
    }

    @External
    public void setSequence(BigInteger seq) {
        if (!Context.getCaller().equals(Context.getOwner())) throw BMVException.unknown("only owner can call this method");
        if (seq.signum() < 0) throw BMVException.unknown("invalid seq. sequence must >= 0");
        var mpProperties = getMessageProofProperties();
        mpProperties.setLastMsgSeq(seq);
        messageProofPropertiesDB.set(mpProperties);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
        logger.println("handleRelayMessage, msg : ", StringUtil.toString(_msg));
        BTPAddress curAddr = BTPAddress.valueOf(_bmc);
        BTPAddress prevAddr = BTPAddress.valueOf(_prev);
        BMVProperties properties = getProperties();
        checkAccessible(curAddr, prevAddr, properties);
        RelayMessage relayMessages = RelayMessage.fromBytes(_msg);
        RelayMessage.TypePrefixedMessage[] typePrefixedMessages = relayMessages.getMessages();
        List<byte[]> msgList = new ArrayList<>();
        LightClientHeader finalizedHeader = null;
        LightClientHeader blockProofHeader = null;
        for (RelayMessage.TypePrefixedMessage message : typePrefixedMessages) {
            Object msg = message.getMessage();
            if (msg instanceof BlockUpdate) {
                logger.println("handleRelayMessage, blockUpdate : " + msg);
                finalizedHeader = processBlockUpdate((BlockUpdate) msg, properties);
            } else if (msg instanceof BlockProof) {
                logger.println("handleRelayMessage, blockProof : " + msg);
                blockProofHeader = processBlockProof((BlockProof) msg, finalizedHeader);
            } else if (msg instanceof MessageProof) {
                logger.println("handleRelayMessage, MessageProof : " + msg);
                var msgs = processMessageProof((MessageProof) msg, blockProofHeader);
                msgList.addAll(msgs);
            }
        }
        var retSize = msgList.size();
        var ret = new byte[retSize][];
        for (int i = 0; i < retSize; i ++)
            ret[i] = msgList.get(i);
        return ret;
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        var mpProperties = getMessageProofProperties();
        BMVStatus s = new BMVStatus();
        var finalizedHeaderSlot = finalizedHeaderDB.get().getBeacon().getSlot();
        s.setHeight(finalizedHeaderSlot.longValue());
        s.setExtra(new BMVStatusExtra(
                mpProperties.getLastMsgSeq(),
                mpProperties.getLastMsgSlot()).toBytes());
        return s;
    }

    @External(readonly = true)
    public String getVersion() {
        return "0.3.0";
    }

    BMVProperties getProperties() {
        return propertiesDB.getOrDefault(BMVProperties.DEFAULT);
    }

    MessageProofProperties getMessageProofProperties() {
        return messageProofPropertiesDB.getOrDefault(MessageProofProperties.DEFAULT);
    }

    SyncCommittee getCurrentSyncCommittee() {
        return currentSyncCommitteeDB.get();
    }

    SyncCommittee getNextSyncCommittee() {
        return nextSyncCommitteeDB.get();
    }

    LightClientHeader getFinalizedHeader() {
        return finalizedHeaderDB.get();
    }

    LightClientHeader getBlockProofHeader() {
        return blockProofHeaderDB.get();
    }

    private LightClientHeader processBlockUpdate(BlockUpdate blockUpdate, BMVProperties properties) {
        validateBlockUpdate(blockUpdate, properties);
        return applyBlockUpdate(blockUpdate);
    }

    private void validateBlockUpdate(BlockUpdate blockUpdate, BMVProperties properties) {
        var attestedBeacon = LightClientHeader.deserialize(blockUpdate.getAttestedHeader()).getBeacon();
        var finalizedBeacon = LightClientHeader.deserialize(blockUpdate.getFinalizedHeader()).getBeacon();
        var signatureSlot = blockUpdate.getSignatureSlot();
        var attestedSlot = attestedBeacon.getSlot();
        var finalizedSlot = finalizedBeacon.getSlot();
        logger.println("validateBlockUpdate, ", "signatureSlot : ", signatureSlot, ", attestedSlot : ", attestedSlot, ", finalizedSlot : ", finalizedSlot);
        if (signatureSlot.compareTo(attestedSlot) <= 0) throw BMVException.unknown("signature slot( + " + signatureSlot + ") must be after attested Slot(" + attestedSlot + ")");
        if (attestedSlot.compareTo(finalizedSlot) < 0) throw BMVException.unknown("attested slot (" + attestedSlot + ") must be after finalized slot(" + finalizedSlot + ")");

        var bmvFinalizedBeacon = getFinalizedHeader().getBeacon();
        var bmvSlot = bmvFinalizedBeacon.getSlot();
        var bmvPeriod = Utils.computeSyncCommitteePeriod(bmvSlot);
        var signaturePeriod = Utils.computeSyncCommitteePeriod(signatureSlot);
        var isBmvPeriod = signaturePeriod.compareTo(bmvPeriod) == 0;
        var bmvNextSyncCommittee = getNextSyncCommittee();

        if (bmvNextSyncCommittee != null) {
            if (!isBmvPeriod && signaturePeriod.compareTo(bmvPeriod.add(BigInteger.ONE)) != 0)
                throw BMVException.notVerifiable(bmvSlot.toString());
        } else {
            if (!isBmvPeriod)
                throw BMVException.notVerifiable(bmvSlot.toString());
        }

        blockUpdate.verifyFinalizedHeader();

        var nextSyncCommittee = blockUpdate.getNextSyncCommittee();
        if (nextSyncCommittee != null) {
            logger.println("validateBlockUpdate, ", "verify nextSyncCommittee aggregatedKey : " + StringUtil.toString(nextSyncCommittee.getAggregatePubKey()));
            blockUpdate.verifyNextSyncCommittee();
        }

        SyncCommittee syncCommittee;
        if (isBmvPeriod) {
            syncCommittee = getCurrentSyncCommittee();
        } else {
            syncCommittee = bmvNextSyncCommittee;
        }
        logger.println("validateBlockUpdate, ", "verify syncAggregate", syncCommittee.getAggregatePubKey());
        if (!blockUpdate.verifySyncAggregate(syncCommittee.getBlsPublicKeys(), properties.getGenesisValidatorsHash()))
            throw BMVException.unknown("invalid signature");
    }

    private LightClientHeader applyBlockUpdate(BlockUpdate blockUpdate) {
        var bmvNextSyncCommittee = getNextSyncCommittee();
        var bmvFinalizedHeader = getFinalizedHeader();
        var bmvBeacon = bmvFinalizedHeader.getBeacon();
        var bmvSlot = bmvBeacon.getSlot();
        var finalizedHeader = LightClientHeader.deserialize(blockUpdate.getFinalizedHeader());
        var finalizedSlot = finalizedHeader.getBeacon().getSlot();
        var bmvPeriod = Utils.computeSyncCommitteePeriod(bmvSlot);
        var finalizedPeriod = Utils.computeSyncCommitteePeriod(finalizedSlot);

        if (bmvNextSyncCommittee == null) {
            if (finalizedPeriod.compareTo(bmvPeriod) != 0) throw BMVException.unknown("invalid update period");
            logger.println("applyBlockUpdate, ", "set next sync committee");
            nextSyncCommitteeDB.set(blockUpdate.getNextSyncCommittee());
        } else if (finalizedPeriod.compareTo(bmvPeriod.add(BigInteger.ONE)) == 0) {
            logger.println("applyBlockUpdate, ", "set current/next sync committee");
            currentSyncCommitteeDB.set(getNextSyncCommittee());
            nextSyncCommitteeDB.set(blockUpdate.getNextSyncCommittee());
        }

        if (finalizedSlot.compareTo(bmvSlot) > 0) {
            logger.println("applyBlockUpdate, ", "set finalized header");
            finalizedHeaderDB.set(finalizedHeader);
            return finalizedHeader;
        }
        return bmvFinalizedHeader;
    }

    private LightClientHeader processBlockProof(BlockProof blockProof, LightClientHeader finalizedHeader) {
        var historicalLimit = BigInteger.valueOf(8192);
        if (finalizedHeader == null) finalizedHeader = getFinalizedHeader();
        var bmvBeacon = finalizedHeader.getBeacon();
        var blockProofLightClientHeader = blockProof.getLightClientHeader();
        var blockProofBeacon = blockProofLightClientHeader.getBeacon();
        var bmvFinalizedSlot = bmvBeacon.getSlot();
        var blockProofSlot = blockProofBeacon.getSlot();
        var blockProofBeaconHashTreeRoot = blockProofBeacon.getHashTreeRoot();
        var bmvStateRoot = bmvBeacon.getStateRoot();
        var proof = blockProof.getProof();
        logger.println("processBlockProof, ", "blockProofSlot : ", blockProofSlot, ", bmvFinalizedSlot : ", bmvFinalizedSlot);
        logger.println("processBlockProof, ", "bmvStateRoot : ", StringUtil.bytesToHex(bmvStateRoot), ", proof : ", proof);
        if (proof == null) {
            if (!bmvBeacon.equals(blockProofBeacon)) {
                throw BMVException.unknown("BlockProof.proof is empty but BlockProof.header is not same with finalized header");
            }
        } else {
            var proofLeaf = proof.getLeaf();
            if (bmvFinalizedSlot.compareTo(blockProofSlot) < 0)
                throw BMVException.unknown(blockProofSlot.toString());
            if (blockProofSlot.add(historicalLimit).compareTo(bmvFinalizedSlot) < 0) {
                var historicalProof = blockProof.getHistoricalProof();
                logger.println("processBlockProof, ", "historicalProof : ", historicalProof);
                if (historicalProof == null)
                    throw BMVException.unknown("historicalProof empty");
                if (!Arrays.equals(blockProofBeaconHashTreeRoot, historicalProof.getLeaf()))
                    throw BMVException.unknown("invalid hashTree");
                SszUtils.verify(bmvStateRoot, proof);
                SszUtils.verify(proofLeaf, historicalProof);
            } else {
                if (!Arrays.equals(proofLeaf, blockProofBeaconHashTreeRoot))
                    throw BMVException.unknown("invalid hashTree");
                SszUtils.verify(bmvStateRoot, proof);
            }
        }
        blockProofHeaderDB.set(blockProofLightClientHeader);
        return blockProofLightClientHeader;
    }

    private List<byte[]> processMessageProof(MessageProof messageProof, LightClientHeader blockProofHeader) {
        var mpProperties = getMessageProofProperties();
        var seq = mpProperties.getLastMsgSeq();
        if (blockProofHeader == null) blockProofHeader = getBlockProofHeader();
        var blockProofBeacon = blockProofHeader.getBeacon();
        var stateRoot = blockProofBeacon.getStateRoot();
        var receiptRootProof = messageProof.getReceiptsRootProof();
        logger.println("processMessageProof, ", "stateRoot", StringUtil.bytesToHex(stateRoot), ", receiptRootProof : ", receiptRootProof);
        SszUtils.verify(stateRoot, receiptRootProof);
        var receiptsRoot = receiptRootProof.getLeaf();
        var ethBmc = mpProperties.getEthBmc();
        var messageList = new ArrayList<byte[]>();
        for (ReceiptProof rp : messageProof.getReceiptProofs()) {
            logger.println("processMessageProof, ", "mpt prove", ", receiptProof key : ", StringUtil.bytesToHex(rp.getKey()));
            var value = MerklePatriciaTree.prove(receiptsRoot, rp.getKey(), rp.getProofs());
            var receipt = Receipt.fromBytes(value);
            logger.println("processMessageProof, ", "receipt : ", receipt);
            for (Log log : receipt.getLogs()) {
                var topics = log.getTopics();
                var signature = topics[0];
                var logAddress = log.getAddress();
                if (!Arrays.equals(ethBmc, logAddress)) continue;
                if (!Arrays.equals(signature, eventSignatureTopic)) continue;
                var msgSeq = new BigInteger(topics[2]);
                seq = seq.add(BigInteger.ONE);
                if (seq.compareTo(msgSeq) != 0) throw BMVException.unknown("invalid message sequence");
                var msg = log.getMessage();
                messageList.add(msg);
            }
        }
        var cnt = messageList.size();
        if (cnt != 0) {
            mpProperties.setLastMsgSeq(seq);
            mpProperties.setLastMsgSlot(blockProofBeacon.getSlot());
            messageProofPropertiesDB.set(mpProperties);
        }
        return messageList;
    }

    private void checkAccessible(BTPAddress curAddr, BTPAddress fromAddress, BMVProperties properties) {
        if (!properties.getNetwork().equals(fromAddress.net())) {
            throw BMVException.unknown("invalid prev bmc");
        } else if (!Context.getCaller().equals(properties.getBmc())) {
            throw BMVException.unknown("invalid caller bmc");
        } else if (!Address.fromString(curAddr.account()).equals(properties.getBmc())) {
            throw BMVException.unknown("invalid current bmc");
        }
    }
}
