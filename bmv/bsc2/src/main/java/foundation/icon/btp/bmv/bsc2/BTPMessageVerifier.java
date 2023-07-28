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
package foundation.icon.btp.bmv.bsc2;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static foundation.icon.btp.bmv.bsc2.Header.*;

public class BTPMessageVerifier implements BMV {
    private final VarDB<Address> bmc = Context.newVarDB("bmc", Address.class);
    private final VarDB<BlockTree> tree = Context.newVarDB("tree", BlockTree.class);
    private final VarDB<Snapshot> snap = Context.newVarDB("snap", Snapshot.class);
    private final VarDB<MerkleTreeAccumulator> mta = Context.newVarDB("mta", MerkleTreeAccumulator.class);
    private final DictDB<byte[], Header> heads = Context.newDictDB("heads", Header.class);

    public BTPMessageVerifier(Address _bmc, BigInteger _chainId, @Optional byte[] _header,
                              @Optional byte[] _validators, @Optional byte[] _candidates, @Optional byte[] _recents) {
        ChainConfig config = ChainConfig.setChainID(_chainId);
        if (_header != null) {
            Header head = Header.fromBytes(_header);
            verify(config, head);

            MerkleTreeAccumulator mta = new MerkleTreeAccumulator();
            mta.setHeight(head.getNumber().longValue());
            mta.setOffset(head.getNumber().longValue());
            mta.add(head.getHash().toBytes());

            Validators validators = Validators.fromBytes(_validators);
            EthAddresses recents = EthAddresses.fromBytes(_recents);
            if (head.getNumber().compareTo(BigInteger.ZERO) == 0) {
                Context.require(recents.size() == 1, "Wrong recent signers");
            } else {
                Context.require(recents.size() == validators.size() / 2 + 1,
                        "Wrong recent signers - validators/2+1");
            }

            this.bmc.set(_bmc);
            this.tree.set(new BlockTree(head.getHash()));
            this.mta.set(mta);
            this.heads.set(head.getHash().toBytes(), head);
            VoteAttestation attestation = head.getVoteAttestation(config);
            Context.require(attestation != null, "No vote attestation");
            this.snap.set(new Snapshot(
                        head.getHash(),
                        head.getNumber(),
                        Validators.fromBytes(_validators),
                        Validators.fromBytes(_candidates),
                        Validators.fromBytes(_validators),
                        EthAddresses.fromBytes(_recents),
                        attestation));
        } else {
            Context.require(_bmc.equals(this.bmc.get()), "Mismatch BMC address");
        }
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        MerkleTreeAccumulator mta = this.mta.get();
        BlockTree tree = this.tree.get();
        Header head = this.heads.get(tree.getRoot().toBytes());
        BMVStatus status = new BMVStatus();
        status.setHeight(head.getNumber().longValue());
        status.setExtra((new BMVStatusExtra(mta.getOffset(), tree)).toBytes());
        return status;
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
        checkAccessible();

        BlockTree tree = this.tree.get();
        MerkleTreeAccumulator mta = this.mta.get();
        ChainConfig config = ChainConfig.getInstance();
        List<Header> confirmations = new ArrayList<>();
        List<MessageEvent> msgs = new ArrayList<>();
        BigInteger seq = _seq.add(BigInteger.ONE);

        RelayMessage rm = RelayMessage.fromBytes(_msg);
        for (RelayMessage.TypePrefixedMessage tpm : rm.getMessages()) {
            Object msg = tpm.getMessage();
            if (msg instanceof BlockUpdate) {
                confirmations.addAll(handleBlockUpdate(config, (BlockUpdate) msg, tree, mta));
            } else if (msg instanceof BlockProof) {
                confirmations.add(handleBlockProof((BlockProof) msg, mta));
            } else if (msg instanceof MessageProof) {
                msgs.addAll(handleMessageProof((MessageProof) msg, confirmations,
                        seq.add(BigInteger.valueOf(msgs.size())),
                        EthAddress.of(BTPAddress.parse(_prev).account()), BTPAddress.parse(_bmc)));
            }
        }

        this.tree.set(tree);
        this.mta.set(mta);

        int i = 0;
        byte[][]ret = new byte[msgs.size()][];
        for (MessageEvent msg : msgs) {
            ret[i++] = msg.getMessage();
        }
        return ret;
    }

    private List<Header> handleBlockUpdate(ChainConfig config, BlockUpdate bu, BlockTree tree, MerkleTreeAccumulator mta) {
        List<Header> newHeads = new ArrayList<>(bu.getHeaders());
        if (newHeads.isEmpty()) {
            return new ArrayList<>();
        }
        List<Hash> ancestors = tree.getStem(newHeads.get(0).getParentHash());
        Context.require(ancestors.size() > 0, "Inconsistent block");

        // load heads in storage
        Snapshot snap = this.snap.get();
        Map<Hash, Header> heads = new HashMap<>();
        Map<Hash, Snapshot> snaps = new HashMap<>();
        for (Hash ancestor : ancestors) {
            Header head = this.heads.get(ancestor.toBytes());
            heads.put(ancestor, head);

            if (snap.getNumber().longValue() + 1L == head.getNumber().longValue()) {
                snap = snap.apply(config, head);
            }
            snaps.put(ancestor, snap);
        }

        Header parent = heads.get(newHeads.get(0).getParentHash());
        for (Header newHead : newHeads) {
            verify(config, newHead, parent, snap);
            if (newHead.getVoteAttestation(config) != null) {
                verifyVoteAttestation(config, newHead, snap);
            }
            tree.add(snap.getHash(), newHead.getHash());
            snap = snap.apply(config, newHead);
            snaps.put(snap.getHash(), snap);
            heads.put(snap.getHash(), newHead);
            parent = newHead;
        }

        // current `parent` refer to leaf header
        Hash finality = getFinalizedBlockHash(heads, snaps, heads.get(tree.getRoot()), parent);
        if (finality == null) {
            return new ArrayList<>();
        }

        this.snap.set(snaps.get(finality));

        // ascending ordered finalized heads
        List<Header> finalities = collect(heads, tree.getRoot(), finality);
        for (Header head : finalities) {
            mta.add(head.getHash().toBytes());
        }

        tree.prune(finality, new BlockTree.OnRemoveListener() {
            @Override
            public void onRemove(Hash node) {
                if (ancestors.contains(node)) {
                    // remove finalized heads on storage
                    BTPMessageVerifier.this.heads.set(node.toBytes(), null);
                } else {
                    // remove finalized heads on memory
                    for (int i = newHeads.size()-1; i >= 0; i--) {
                        if (newHeads.get(i).getHash().equals(node)) {
                            newHeads.remove(i);
                            break;
                        }
                    }
                }
            }
        });

        // store not finalized heads
        for (Header newHead : newHeads) {
            this.heads.set(newHead.getHash().toBytes(), newHead);
        }
        return finalities;
    }

    private Header handleBlockProof(BlockProof bp, MerkleTreeAccumulator mta) {
        Header head = bp.getHeader();
        if (head.getNumber().compareTo(BigInteger.valueOf(mta.getHeight())) > 0) {
            throw BMVException.unknown("Invalid block proof height - " +
                    "avail: " + mta.getHeight() + " input: " + head.getNumber());
        }

        try {
            mta.verify(bp.getWitness(), head.getHash().toBytes(),
                    head.getNumber().longValue()+1, bp.getHeight().intValue());
        } catch (MTAException.InvalidWitnessOldException e) {
            throw BMVException.invalidBlockWitnessOld(e.getMessage());
        } catch (MTAException e) {
            throw BMVException.unknown(e.getMessage());
        }

        return head;
    }

    private List<MessageEvent> handleMessageProof(MessageProof mp, List<Header> confirmations,
                                                  BigInteger seq, EthAddress prev, BTPAddress bmc) {
        List<MessageEvent> msgs = new ArrayList<>();
        if (confirmations.isEmpty()) {
            return msgs;
        }

        Header head = null;
        for (Header confirmation : confirmations) {
            if (confirmation.getHash().equals(mp.getId())) {
                head = confirmation;
                break;
            }
        }

        Context.require(head != null, "No confirmed header for message proof");
        for (ReceiptProof rp : mp.getReceiptProofs()) {
            Receipt receipt;
            byte[] receiptBytes;
            try {
                receiptBytes = MerklePatriciaTree.prove(
                        head.getReceiptHash().toBytes(), rp.getKey(), rp.getProof());
            } catch (MerklePatriciaTree.MPTException e) {
                throw BMVException.unknown(e.getMessage());
            }

            receipt = Receipt.fromBytes(receiptBytes);
            Context.require(receipt.getStatus() != Receipt.StatusFailed, "Failed receipt");
            for (EventLog log : receipt.getLogs()) {
                if (!log.getAddress().equals(prev)) {
                    continue;
                }

                if (!MessageEvent.SIGNATURE.equals(log.getSignature())) {
                    continue;
                }

                MessageEvent msg = MessageEvent.of(bmc, log);
                if (!msg.getNext().equals(bmc)) {
                    continue;
                }

                if (msg.getSequence().compareTo(seq) < 0) {
                    continue;
                }

                if (msg.getSequence().compareTo(seq) > 0) {
                    throw BMVException.notVerifiable("expected:" + seq + " actual:" + msg.getSequence());
                }

                seq = seq.add(BigInteger.ONE);
                msgs.add(msg);
            }
        }
        return msgs;
    }

    private void verify(ChainConfig config, Header head) {
        Context.require(head.getNumber().compareTo(BigInteger.ZERO) >= 0, "Unknown block");
        Context.require(head.getUncleHash().equals(UNCLE_HASH), "Invalid uncle hash");
        if (head.getNumber().compareTo(BigInteger.ZERO) != 0) {
            Context.require(head.getDifficulty().compareTo(BigInteger.ZERO) != 0, "Invalid difficulty");
        }
        byte[] extra = head.getExtra();
        Context.require(extra.length >= EXTRA_VANITY + EXTRA_SEAL, "Missing vanity/seal fields");
        if (config.isEpoch(head.getNumber())) {
            Context.require(extra.length > EXTRA_VANITY + EXTRA_SEAL, "Missing validators set bytes");
        }
        Context.require(head.getMixDigest().equals(Hash.EMPTY), "Invalid mix digest" + head.getMixDigest());
        Context.require(head.getGasLimit().compareTo(MIN_GAS_LIMIT) >= 0, "Invalid gas limit(< min)");
        Context.require(head.getGasLimit().compareTo(MAX_GAS_LIMIT) <= 0, "Invalid gas limit(> max)");
        Context.require(head.getGasUsed().compareTo(head.getGasLimit()) < 0, "Invalid gas used");
        Context.require(head.getSigner(BigInteger.valueOf(config.ChainID)).equals(head.getCoinbase()), "Coinbase mismatch");
    }

    private void verify(ChainConfig config, Header head, Header parent, Snapshot snap) {
        verify(config, head);

        // verify cascading fields
        if (head.getNumber().equals(BigInteger.ZERO)) {
            return;
        }

        Context.require(parent.getNumber().add(BigInteger.ONE).equals(head.getNumber()),
                "Inconsistent block number");
        Context.require(parent.getHash().equals(head.getParentHash()), "Inconsistent block hash");

        verifyForRamanujanFork(config, snap, head, parent);

        BigInteger diff = parent.getGasLimit().add(head.getGasLimit().negate());
        if (diff.compareTo(BigInteger.ZERO) < 0) {
            diff = diff.negate();
        }
        Context.require(diff.compareTo(parent.getGasLimit().divide(GAS_LIMIT_BOUND_DIVISOR)) < 0,
                "Invalid gas limit");

        Context.require(snap.getValidators().getAddresses().contains(head.getCoinbase()), "Unauthorized validator");
        Context.require(!snap.getRecents().contains(head.getCoinbase()) ||
                snap.getRecents().size() <= snap.getValidators().size() / 2 + 1, "Recently signed");
        if (snap.inturn(head.getCoinbase())) {
            Context.require(head.getDifficulty().equals(INTURN_DIFF), "Wrong difficulty(in-turn)");
        } else {
            Context.require(head.getDifficulty().equals(NOTURN_DIFF), "Wrong difficulty(no-turn)");
        }
    }

    private void verifyVoteAttestation(ChainConfig config, Header head, Snapshot snap) {
        VoteAttestation atte = head.getVoteAttestation(config);
        if (atte == null) {
            return;
        }

        Context.require(head.getParentHash().equals(snap.getHash()),
                "Invalid snapshot, no parent snapshot");
        Context.require(atte.getVoteRange() != null, "Invalid attestation, vote range is null");

        // target block should be direct parent
        Context.require(atte.isTargetOf(snap.getNumber(), snap.getHash()),
                "Invalid attestation, target mismatch");
        VoteRange pvr = snap.getVoteAttestation().getVoteRange();
        Context.require(atte.isSourceOf(pvr.getTargetNumber(), pvr.getTargetHash()),
                "Invalid attestation, source mismatch");
        atte.verify(snap.getVoters());
    }

    private Hash getFinalizedBlockHash(Map<Hash, Header> heads, Map<Hash, Snapshot> snaps, Header root, Header from) {
        Snapshot snap = snaps.get(from.getHash());
        Header head = heads.get(from.getHash());
        while (snap != null && !snap.getHash().equals(root.getHash())) {
            VoteRange range = snap.getVoteAttestation().getVoteRange();
            if (range.getTargetNumber().compareTo(range.getSourceNumber().add(BigInteger.ONE)) == 0) {
                Context.require(snaps.get(range.getSourceHash()) != null, "Unknown justified block hash");
                return range.getSourceHash();
            }
            snap = snaps.get(head.getParentHash());
            head = heads.get(head.getParentHash());
        }
        return null;
    }

    private static final long DEFAULT_BACKOFF_TIME = 1L;
    private void verifyForRamanujanFork(ChainConfig config, Snapshot snap, Header head, Header parent) {
        long diffTime = config.Period + getBackOffTime(snap, head);
        Context.require(head.getTime() >= parent.getTime() + diffTime, "Future block - number("+head.getNumber()+")");
    }

    private long getBackOffTime(Snapshot snap, Header head) {
        if (snap.inturn(head.getCoinbase())) {
            return 0L;
        }

        Validators vals = snap.getValidators();
        long number = head.getNumber().longValue();
        EthAddress inturn = vals.get((int)(number % (long)vals.size())).getAddress();
        if (snap.getRecents().contains(inturn)) {
            return 0L;
        }
        return DEFAULT_BACKOFF_TIME;
    }

    private void checkAccessible() {
        if (!Context.getCaller().equals(this.bmc.get())) {
            throw BMVException.unknown("invalid caller bmc");
        }
    }

    private static List<Header> collect(Map<Hash, Header> heads, Hash from, Hash to) {
        List<Header> cols = new ArrayList<>();
        Header head = heads.get(to);
        while (!head.getHash().equals(from)) {
            cols.add(head);
            head = heads.get(head.getParentHash());
            Context.require(head != null, "Inconsistent chain");
        }
        reverse(cols);
        return cols;
    }

    private static void reverse(List<Header> heads) {
        for (int i = 0; i < heads.size() / 2; i++) {
            Header tmp = heads.get(i);
            heads.set(i, heads.get(heads.size()-1-i));
            heads.set(heads.size()-1-i, tmp);
        }
    }
}
