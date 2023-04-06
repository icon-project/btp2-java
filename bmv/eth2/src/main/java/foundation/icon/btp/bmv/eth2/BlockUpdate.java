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

import foundation.icon.score.util.StringUtil;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class BlockUpdate {
    private byte[] attestedHeader;
    private byte[] finalizedHeader;
    private byte[][] finalizedHeaderBranch;
    private byte[] syncAggregate;
    private BigInteger signatureSlot;
    private byte[] nextSyncCommittee;
    private byte[][] nextSyncCommitteeBranch;
    private static final byte[] DOMAIN_SYNC_COMMITTEE = StringUtil.hexToBytes("07000000");
    private static final String BLS_AGGREGATE_ALG = "bls12-381-g1";
    private static final String BLS_SIG_ALG = "bls12-381-g2";

    public BlockUpdate(
            byte[] attestedHeader,
            byte[] finalizedHeader,
            byte[][] finalizedHeaderBranch,
            byte[] syncAggregate,
            BigInteger signatureSlot,
            byte[] nextSyncCommittee,
            byte[][] nextSyncCommitteeBranch
    ) {
        this.attestedHeader = attestedHeader;
        this.finalizedHeader = finalizedHeader;
        this.finalizedHeaderBranch = finalizedHeaderBranch;
        this.syncAggregate = syncAggregate;
        this.signatureSlot = signatureSlot;
        this.nextSyncCommittee = nextSyncCommittee;
        this.nextSyncCommitteeBranch = nextSyncCommitteeBranch;
    }

    public byte[] getFinalizedHeader() {
        return finalizedHeader;
    }

    public byte[] getAttestedHeader() {
        return attestedHeader;
    }

    SyncAggregate getSyncAggregate() {
        return SyncAggregate.deserialize(syncAggregate);
    }

    SyncCommittee getNextSyncCommittee() {
        if (nextSyncCommittee == null) return null;
        return SyncCommittee.deserialize(nextSyncCommittee);
    }

    void verifyFinalizedHeader() {
        var finalizedHeader = LightClientHeader.deserialize(this.finalizedHeader);
        var attestedHeader = LightClientHeader.deserialize(this.attestedHeader);
        var finalizeHeaderDepth = 6;
        var finalizeHeaderIndex = 41;
        SszUtils.validateMerkleBranch(
                finalizedHeader.getBeacon().getHashTreeRoot(),
                finalizedHeaderBranch,
                finalizeHeaderDepth,
                finalizeHeaderIndex,
                attestedHeader.getBeacon().getStateRoot()
        );
    }

    void verifyNextSyncCommittee() {
        var attestedHeader = LightClientHeader.deserialize(this.attestedHeader);
        int nextSyncCommitteeDepth = 5;
        int nextSyncCommitteeIndex = 23;
        SszUtils.validateMerkleBranch(
                nextSyncCommittee,
                nextSyncCommitteeBranch,
                nextSyncCommitteeDepth,
                nextSyncCommitteeIndex,
                attestedHeader.getBeacon().getStateRoot()
        );
    }

    BigInteger getSignatureSlot() {
        return signatureSlot;
    }

    static BlockUpdate readObject(ObjectReader r) {
        r.beginList();
        var attestedHeader = r.readByteArray();
        var finalizedHeader = r.readByteArray();
        r.beginList();
        List<byte[]> headerBranchList = new ArrayList<>();
        while(r.hasNext())
            headerBranchList.add(r.readByteArray());
        r.end();
        var branchSize = headerBranchList.size();
        byte[][] headerBranch = new byte[branchSize][];
        for (int i = 0; i < branchSize; i++)
            headerBranch[i] = headerBranchList.get(i);
        var syncAggregate = r.readByteArray();
        var signatureSlot = r.readBigInteger();
        var nextSyncCommittee = r.readNullable(byte[].class);
        List<byte[]> committeeBranchList = new ArrayList<>();
        r.beginList();
        while(r.hasNext())
            committeeBranchList.add(r.readByteArray());
        r.end();
        branchSize = committeeBranchList.size();
        byte[][] committeeBranch = new byte[branchSize][];
        for (int i = 0; i < branchSize; i++)
            committeeBranch[i] = committeeBranchList.get(i);
        r.end();
        return new BlockUpdate(attestedHeader, finalizedHeader, headerBranch, syncAggregate, signatureSlot, nextSyncCommittee, committeeBranch);
    }

    public static BlockUpdate fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockUpdate.readObject(reader);
    }

    byte[] getSigningRoot(byte[] genesisValidatorsRoot, BigInteger signatureSlot) {
        var domain = computeDomain(genesisValidatorsRoot, signatureSlot);
        var beacon = LightClientHeader.deserialize(attestedHeader).getBeacon();
        var hashTree = beacon.getHashTreeRoot();
        return SszUtils.concatAndHash(hashTree, domain);
    }

    private static byte[] computeDomain(byte[] genesisValidatorsRoot, BigInteger signatureSlot) {
        var forkDataRoot = computeForkDataRoot(genesisValidatorsRoot, signatureSlot);
        var leaf2 = new byte[28];
        System.arraycopy(forkDataRoot, 0, leaf2, 0, 28);
        return SszUtils.concat(DOMAIN_SYNC_COMMITTEE, leaf2);
    }

    private static byte[] computeForkDataRoot(byte[] genesisValidatorsRoot, BigInteger signatureSlot) {
        var leaf1 = new byte[Constants.HASH_LENGTH];
        var version = BlockUpdate.computeForkVersion(Utils.computeEpoch(signatureSlot), genesisValidatorsRoot);
        System.arraycopy(version, 0, leaf1, 0, Constants.SEPOLIA_BELLATRIX_VERSION.length);
        return SszUtils.concatAndHash(leaf1, genesisValidatorsRoot);
    }

    private static byte[] computeForkVersion(BigInteger epoch, byte[] genesisValidatorsRoot) {
        if (Arrays.equals(genesisValidatorsRoot, Constants.MAINNET_GENESIS_VALIDATORS_ROOT)) {
            if (epoch.compareTo(Constants.MAINNET_CAPELLA_EPOCH) >= 0)
                return Constants.MAINNET_CAPELLA_VERSION;
            if (epoch.compareTo(Constants.MAINNET_BELLATRIX_EPOCH) >= 0)
                return Constants.MAINNET_BELLATRIX_VERSION;
            if (epoch.compareTo(Constants.MAINNET_ALTAIR_EPOCH) >= 0)
                return Constants.MAINNET_ALTAIR_VERSION;
            return Constants.MAINNET_GENESIS_VERSION;
        } else if (Arrays.equals(genesisValidatorsRoot, Constants.SEPOLIA_GENESIS_VALIDATORS_ROOT)) {
            if (epoch.compareTo(Constants.SEPOLIA_CAPELLA_EPOCH) >= 0)
                return Constants.SEPOLIA_CAPELLA_VERSION;
            if (epoch.compareTo(Constants.SEPOLIA_BELLATRIX_EPOCH) >= 0)
                return Constants.SEPOLIA_BELLATRIX_VERSION;
            if (epoch.compareTo(Constants.SEPOLIA_ALTAIR_EPOCH) >= 0)
                return Constants.SEPOLIA_ALTAIR_VERSION;
            return Constants.SEPOLIA_GENESIS_VERSION;
        }
        throw BMVException.unknown("invalid genesisValidatorsRoot");
    }

    boolean verifySyncAggregate(byte[][] syncCommitteePubs, byte[] genesisValidatorsRoot) {
        var syncAggregate = getSyncAggregate();
        var aggregateBits = syncAggregate.getSyncCommitteeBits();
        var signingRoot = getSigningRoot(genesisValidatorsRoot, signatureSlot);
        var committeeSig = syncAggregate.getSyncCommitteeSignature();
        var aggregatedKey = Context.aggregate(BLS_AGGREGATE_ALG, null, new byte[0]);
        var verified = 0;
        for (int i = 0; i < aggregateBits.length; i++) {
            if (aggregateBits[i]) {
                aggregatedKey = Context.aggregate(BLS_AGGREGATE_ALG, aggregatedKey, syncCommitteePubs[i]);
                verified++;
            }
        }
        if (verified * 3 < 2 * aggregateBits.length) {
            throw BMVException.unknown("not enough validator : " + verified);
        }
        return Context.verifySignature(BLS_SIG_ALG, signingRoot, committeeSig, aggregatedKey);
    }

    @Override
    public String toString() {
        return "BlockUpdate{" +
                "attestedHeader=" + StringUtil.toString(attestedHeader) +
                ", finalizedHeader=" + StringUtil.toString(finalizedHeader) +
                ", finalizedHeaderBranch=" + StringUtil.toString(finalizedHeaderBranch) +
                ", syncAggregate=" + StringUtil.toString(syncAggregate) +
                ", signatureSlot=" + signatureSlot +
                ", nextSyncCommittee=" + StringUtil.toString(nextSyncCommittee) +
                ", nextSyncCommitteeBranch=" + StringUtil.toString(nextSyncCommitteeBranch) +
                '}';
    }
}
