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
package foundation.icon.btp.bmv.bsc;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.MTAException;
import foundation.icon.btp.lib.MerklePatriciaTree;
import foundation.icon.btp.lib.MerkleTreeAccumulator;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static foundation.icon.btp.bmv.bsc.Header.*;

public class BTPMessageVerifier implements BMV {
    private final VarDB<Address> bmc = Context.newVarDB("bmc", Address.class);
    private final VarDB<BigInteger> cid = Context.newVarDB("cid", BigInteger.class);
    private final VarDB<BlockTree> tree = Context.newVarDB("tree", BlockTree.class);
    private final VarDB<Snapshot> snap = Context.newVarDB("snap", Snapshot.class);
    private final VarDB<MerkleTreeAccumulator> mta = Context.newVarDB("mta", MerkleTreeAccumulator.class);
    private final DictDB<byte[], Header> heads = Context.newDictDB("heads", Header.class);

    public BTPMessageVerifier(Address bmc, BigInteger chainId, byte[] header,
                              byte[][] recents, byte[][] validators) {

        ChainConfig config = ChainConfig.fromChainID(chainId);
        Header head = Header.fromBytes(header);
        Context.require(config.isEpoch(head.getNumber()), "No epoch block");
        verify(config, head);

        MerkleTreeAccumulator mta = new MerkleTreeAccumulator(head.getNumber().longValueExact());
        mta.add(head.getHash().toBytes());

        if (head.getNumber().compareTo(BigInteger.ZERO) == 0) {
            Context.require(recents.length == 1, "Wrong recent signers");
        } else {
            Context.require(recents.length == validators.length / 2 + 1,
                    "Wrong recent signers - validators/2+1");
        }

        this.bmc.set(bmc);
        this.cid.set(chainId);
        this.tree.set(new BlockTree(head.getHash()));
        this.mta.set(mta);
        this.heads.set(head.getHash().toBytes(), head);
        this.snap.set(new Snapshot(
                head.getHash(),
                head.getNumber(),
                new EthAddresses(toSortedList(validators)),
                new EthAddresses(head.getValidators(config)),
                new EthAddresses(toSortedList(recents))));
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
        ChainConfig config = ChainConfig.fromChainID(this.cid.get());
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

        Hash hash = newHeads.get(0).getParentHash();
        List<Hash> ancestors = tree.getStem(hash);
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

        Header parent = heads.get(hash);
        for (Header newHead : newHeads) {
            verify(config, newHead, parent, snap);
            tree.add(snap.getHash(), newHead.getHash());
            snap = snap.apply(config, newHead);
            snaps.put(snap.getHash(), snap);
            heads.put(snap.getHash(), newHead);
            parent = newHead;
        }

        List<Header> confirmations = confirm(config, snaps, heads, snap.getHash());
        if (confirmations.size() > 0) {
            Hash newRoot = confirmations.get(confirmations.size()-1).getHash();
            this.snap.set(snaps.get(newRoot));
            tree.prune(newRoot, new BlockTree.OnRemoveListener() {
                @Override
                public void onRemove(Hash node) {
                    if (ancestors.contains(node)) {
                        BTPMessageVerifier.this.heads.set(node.toBytes(), null);
                    } else {
                        for (Header newHead : newHeads) {
                            if (newHead.getHash().equals(node)) {
                                newHeads.remove(newHead);
                                break;
                            }
                        }
                    }
                }
            });
            for (Header newHead : newHeads) {
                this.heads.set(newHead.getHash().toBytes(), newHead);
            }
            for (Header confirmation : confirmations) {
                mta.add(confirmation.getHash().toBytes());
            }
        }
        return confirmations;
    }

    private Header handleBlockProof(BlockProof bp, MerkleTreeAccumulator mta) {
        Header head = bp.getHeader();
        if (head.getNumber().compareTo(BigInteger.valueOf(mta.getHeight())) > 0) {
            throw BMVException.unknown("Invalid block proof height - " +
                    "avail: " + mta.getHeight() + " input: " + head.getNumber());
        }

        try {
            mta.verify(bp.getWitness(), head.getHash().toBytes(),
                    head.getNumber().longValue(), bp.getHeight().intValue());
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
        Context.require(extra.length >= EXTRA_VANITY, "Missing signer vanity");
        Context.require(extra.length >= EXTRA_VANITY + EXTRA_SEAL, "Missing signer seal");
        byte[] signersBytes = config.getValidatorBytesFromHeader(head);
        if (config.isEpoch(head.getNumber())) {
            Context.require(signersBytes.length != 0, "Malformed validators set bytes");
        } else {
            Context.require(signersBytes.length == 0, "Forbidden validators set bytes");
        }
        Context.require(head.getMixDigest().equals(Hash.EMPTY), "Invalid mix digest" + head.getMixDigest());
        Context.require(head.getGasLimit().compareTo(MIN_GAS_LIMIT) >= 0, "Invalid gas limit(< min)");
        Context.require(head.getGasLimit().compareTo(MAX_GAS_LIMIT) <= 0, "Invalid gas limit(> max)");
        Context.require(head.getGasUsed().compareTo(head.getGasLimit()) < 0, "Invalid gas used");
        EthAddress signer = head.getSigner(BigInteger.valueOf(config.ChainID));
        Context.require(signer.equals(head.getCoinbase()), "Coinbase mismatch");
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

        Context.require(snap.getValidators().contains(head.getCoinbase()), "Unauthorized validator");
        Context.require(!snap.getRecents().contains(head.getCoinbase()) ||
                snap.getRecents().size() <= snap.getValidators().size() / 2 + 1, "Recently signed");
        if (snap.inturn(head.getCoinbase())) {
            Context.require(head.getDifficulty().equals(INTURN_DIFF), "Wrong difficulty(in-turn)");
        } else {
            Context.require(head.getDifficulty().equals(NOTURN_DIFF), "Wrong difficulty(no-turn)");
        }
    }

    // sorted by leaf to root
    private List<Header> confirm(ChainConfig config, Map<Hash, Snapshot> snaps, Map<Hash, Header> heads, Hash leaf) {
        List<Header> confirmations = new ArrayList<>();
        Header head = heads.get(leaf);
        Snapshot snap = snaps.get(head.getParentHash());
        Map<EthAddress, Boolean> validators = new HashMap<>();
        while (snap != null) {
            EthAddresses newValidators = snap.getCandidates();
            validators.put(head.getCoinbase(), Boolean.TRUE);
            if (config.isEpoch(head.getNumber())) {
                EthAddresses oldValidators = snap.getValidators();
                if (validators.size() > oldValidators.size() / 2 &&
                        countBy(validators, newValidators) > newValidators.size() * 2 / 3) {
                    confirmations.add(heads.get(head.getHash()));
                } else {
                    confirmations.clear();
                }
            } else if (!confirmations.isEmpty() ||
                    countBy(validators, newValidators) > newValidators.size() * 2 / 3) {
                confirmations.add(heads.get(head.getHash()));
            }
            head = heads.get(head.getParentHash());
            snap = snaps.get(head.getParentHash());
        }

        for (int i = 0; i < confirmations.size()/2; i++) {
            Header tmp = confirmations.get(i);
            confirmations.set(i, confirmations.get(confirmations.size()-1-i));
            confirmations.set(confirmations.size()-1-i, tmp);
        }
        return confirmations;
    }

    private static int countBy(Map<EthAddress, Boolean> vals, EthAddresses newVals) {
        int cnt = 0;
        for (int i = 0; i < newVals.size(); i++) {
            if (vals.containsKey(newVals.get(i))) {
                cnt++;
            }
        }
        return cnt;
    }

    private static final long DEFAULT_BACKOFF_TIME = 1L;
    private void verifyForRamanujanFork(ChainConfig config, Snapshot snap, Header head, Header parent) {
        if (!config.isRamanujan(head.getNumber())) {
            return;
        }

        long diffTime = config.Period + getBackOffTime(config, snap, head);
        Context.require(head.getTime() >= parent.getTime() + diffTime, "Future block - number("+head.getNumber()+")");
    }

    private long getBackOffTime(ChainConfig config, Snapshot snap, Header head) {
        if (snap.inturn(head.getCoinbase())) {
            return 0L;
        }

        if (config.isPlanck(head.getNumber())) {
            EthAddresses vals = snap.getValidators();
            long number = head.getNumber().longValue();
            EthAddress inturn = vals.get((int)(number % (long)vals.size()));
            if (snap.getRecents().contains(inturn)) {
                return 0L;
            }
        }
        return DEFAULT_BACKOFF_TIME;
    }

    private void checkAccessible() {
        if (!Context.getCaller().equals(this.bmc.get())) {
            throw BMVException.unknown("invalid caller bmc");
        }
    }

    private List<EthAddress> toSortedList(byte[][] addrs) {
        List<EthAddress> list = new ArrayList<>();
        for (int i = 0; i < addrs.length; i++) {
            list.add(new EthAddress(addrs[i]));
        }
        EthAddresses.sort(list);
        return list;
    }
}
