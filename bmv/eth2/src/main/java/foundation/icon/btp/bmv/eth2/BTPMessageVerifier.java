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
    private final VarDB<BMVProperties> propertiesDB = Context.newVarDB("properties", BMVProperties.class);
    private final String eventSignature = "Message(string,uint256,bytes)";
    private final byte[] eventSignatureTopic = Context.hash("keccak-256", eventSignature.getBytes());

    public BTPMessageVerifier(String srcNetworkID, byte[] genesisValidatorsHash, byte[] syncCommittee, Address bmc, byte[] finalizedHeader, byte[] etherBmc) {
        var properties = getProperties();
        properties.setSrcNetworkID(srcNetworkID.getBytes());
        properties.setBmc(bmc);
        properties.setGenesisValidatorsHash(genesisValidatorsHash);
        properties.setCurrentSyncCommittee(syncCommittee);
        properties.setFinalizedHeader(BeaconBlockHeader.deserialize(finalizedHeader));
        properties.setEtherBmc(etherBmc);
        properties.setLastMsgSlot(BigInteger.ZERO);
        properties.setLastMsgSeq(BigInteger.ZERO);
        propertiesDB.set(properties);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
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
                processBlockUpdate((BlockUpdate) msg);
            } else if (msg instanceof BlockProof) {
                blockProof = (BlockProof) msg;
                processBlockProof(blockProof);
            } else if (msg instanceof MessageProof) {
                var msgs = processMessageProof((MessageProof) msg, blockProof);
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
        s.setExtra(new BMVStatusExtra(
                properties.getFinalizedHeader().getSlot(),
                properties.getLastMsgSeq(),
                properties.getLastMsgSlot()).toBytes());
        return s;
    }

    @External
    public void setEtherBMC(byte[] etherBMC) {
        var bmvProperties = getProperties();
        bmvProperties.setEtherBmc(etherBMC);
        propertiesDB.set(bmvProperties);
    }

    BMVProperties getProperties() {
        return propertiesDB.getOrDefault(BMVProperties.DEFAULT);
    }

    private void processBlockUpdate(BlockUpdate blockUpdate) {
        blockUpdate.verifyFinalizedHeader();
        var bmvProperties = getProperties();

        var currentSyncCommittee = SyncCommittee.deserialize(bmvProperties.getCurrentSyncCommittee());
        if (!blockUpdate.verifySyncAggregate(currentSyncCommittee.getBlsPublicKeys(), bmvProperties.getGenesisValidatorsHash(), blockUpdate.getSignatureSlot()))
            throw BMVException.unknown("invalid signature");

        var nextSyncCommittee = blockUpdate.getNextSyncCommittee();
        if (nextSyncCommittee != null)
            blockUpdate.verifyNextSyncCommittee();

        applyBlockUpdate(blockUpdate, bmvProperties);
    }

    private void applyBlockUpdate(BlockUpdate blockUpdate, BMVProperties bmvProperties) {
        var nextSyncCommittee = bmvProperties.getNextSyncCommittee();
        var storeSlot = bmvProperties.getFinalizedHeader().getSlot();
        var updateFinalizedSlot = BeaconBlockHeader.deserialize(blockUpdate.getFinalizedHeader()).getSlot();
        var storePeriod = Utils.computeSyncCommitteePeriod(Utils.computeEpoch(storeSlot));
        var updateFinalizedPeriod = Utils.computeSyncCommitteePeriod(Utils.computeEpoch(updateFinalizedSlot));
        if (nextSyncCommittee == null) {
            if (updateFinalizedPeriod.compareTo(storePeriod) == 0) throw BMVException.unknown("invalid update period");
            bmvProperties.setNextSyncCommittee(blockUpdate.getNextSyncCommittee().toBytes());
        } else if (updateFinalizedPeriod.compareTo(storePeriod.add(BigInteger.ONE)) == 0) {
            bmvProperties.setCurrentSyncCommittee(bmvProperties.getNextSyncCommittee());
            bmvProperties.setNextSyncCommittee(blockUpdate.getNextSyncCommittee().toBytes());
        }
        if (updateFinalizedPeriod.compareTo(bmvProperties.getFinalizedHeader().getSlot()) > 0) {
            bmvProperties.setFinalizedHeader(BeaconBlockHeader.deserialize(blockUpdate.getFinalizedHeader()));
        }
        propertiesDB.set(bmvProperties);
    }

    private void processBlockProof(BlockProof blockProof) {
        var bmvProperties = getProperties();
        var finalizedHeader = bmvProperties.getFinalizedHeader();
        var attestingHeader = blockProof.getBeaconBlockHeader();
        var finalizedSlot = finalizedHeader.getSlot();
        var blockProofSlot = attestingHeader.getSlot();
        if (finalizedSlot.compareTo(blockProofSlot) < 0)
            throw BMVException.invalidBlockProofSlot(blockProofSlot.toString());
        var hashTree = attestingHeader.getHashTreeRoot();
        var proofLeaf = blockProof.getProof().getLeaf();
        if (!Arrays.equals(proofLeaf, hashTree))
            throw BMVException.unknown("invalid hashTree");
        SszUtils.verify(finalizedHeader.getStateRoot(), blockProof.getProof());
    }

    private byte[][] processMessageProof(MessageProof messageProof, BlockProof blockProof) {
        var bmvProperties = getProperties();
        var beaconBlockHeader = blockProof.getBeaconBlockHeader();
        var stateRoot = beaconBlockHeader.getStateRoot();
        var receiptRootProof = messageProof.getReceiptRootProof();
        SszUtils.verify(stateRoot, receiptRootProof);
        var receiptsRoot = receiptRootProof.getLeaf();
        var messageList = new ArrayList<byte[]>();
        for (ReceiptProof rp : messageProof.getReceiptProofs()) {
            var value = MerklePatriciaTree.prove(receiptsRoot, rp.getKey(), rp.getProofs());
            var receipt = Receipt.fromBytes(value);
            for (Log log : receipt.getLogs()) {
                var topic = log.getTopics()[0];
                if (Arrays.equals(log.getAddress(), bmvProperties.getEtherBmc()) && Arrays.equals(topic, eventSignatureTopic)) {
                    var msg = messageFromData(log.getData());
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

    private byte[] messageFromData(byte[] data) {
        var params = abiDecode(StringUtil.bytesToHex(data));
        return (byte[])params.get(2);
    }

    private List<Object> abiDecode(String eventData) {
        String[] inputTypes = {"string", "uint256", "bytes"};
        List<Object> decoded = new ArrayList<>();

        int offset = 0;
        for (String inputType : inputTypes) {
            switch (inputType) {
                case "bytes":
                    int byteLength = Integer.parseInt(eventData.substring(offset, offset + 64), 16);
                    int byteStart = (byteLength * 2) + 64 + offset;
                    byte[] bytes = StringUtil.hexToBytes(eventData.substring(byteStart, byteStart + (byteLength * 2)));
                    decoded.add(bytes);
                    offset += (byteLength * 2) + 64;
                    break;
                case "string":
                    int stringLength = Integer.parseInt(eventData.substring(offset, offset + 64), 16);
                    int stringStart = (stringLength * 2) + 64 + offset;
                    String string = new String(StringUtil.hexToBytes(eventData.substring(stringStart, stringStart + (stringLength * 2))));
                    decoded.add(string);
                    offset += (stringLength * 2) + 64;
                    break;
                case "uint256":
                    BigInteger uint256 = new BigInteger(eventData.substring(offset, offset + 64), 16);
                    decoded.add(uint256.toByteArray());
                    offset += 64;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid input type: " + inputType);
            }
        }
        return decoded;
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
