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
import score.*;
import score.annotation.External;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static foundation.icon.btp.bmv.bsc.Header.*;

public class BTPMessageVerifier implements BMV {
    private VarDB<BlockTree> tree = Context.newVarDB("trie", BlockTree.class);
    private VarDB<MerkleTreeAccumulator> mta = Context.newVarDB("mta", MerkleTreeAccumulator.class);
    private DictDB<Hash, Header> heads = Context.newDictDB("heads", Header.class);
    private DictDB<Hash, Snapshot> snapshots = Context.newDictDB("snapshots", Snapshot.class);

    public BTPMessageVerifier(BigInteger chainId, BigInteger epoch, byte[] header,
                              String recents, String validators) {

        Config.setOnce(Config.CHAIN_ID, chainId);
        Config.setOnce(Config.EPOCH, epoch);

        Header head = Header.fromBytes(header);
        Context.require(head.isEpochBlock(), "No epoch block");
        verify(head);

        MerkleTreeAccumulator mta = new MerkleTreeAccumulator();
        mta.setHeight(head.getNumber().longValue());
        mta.setOffset(head.getNumber().longValue());
        mta.add(head.getHash().toBytes());

        EthAddresses _validators = EthAddresses.fromString(validators);
        EthAddresses _recents = EthAddresses.fromString(recents);
        if (head.getNumber().compareTo(BigInteger.ZERO) == 0) {
            Context.require(_recents.size() == 1, "Wrong recent signers");
        } else {

            Context.require(_recents.size() == _validators.size() / 2 + 1,
                    "Wrong recent signers - validators/2+1");
        }

        this.tree.set(new BlockTree(head.getHash()));
        this.mta.set(mta);
        this.heads.set(head.getHash(), head);
        this.snapshots.set(head.getHash(), new Snapshot(
                head.getHash(),
                head.getNumber(),
                new EthAddresses(_validators),
                new EthAddresses(head.getValidators()),
                new EthAddresses(_recents)));
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        MerkleTreeAccumulator mta = this.mta.get();
        BlockTree trie = this.tree.get();
        Header head = heads.get(trie.getRoot());
        BMVStatus status = new BMVStatus();
        status.setHeight(head.getNumber().longValue());
        status.setExtra((new BMVStatusExtra(mta.getOffset(), trie)).toBytes());
        return status;
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
        BlockTree trie = this.tree.get();
        MerkleTreeAccumulator mta = this.mta.get();
        List<Header> confirmations = new ArrayList<>();
        List<MessageEvent> msgs = new ArrayList<>();
        BigInteger seq = _seq.add(BigInteger.ONE);

        RelayMessage rm = RelayMessage.fromBytes(_msg);
        for (RelayMessage.TypePrefixedMessage tpm : rm.getMessages()) {
            Object msg = tpm.getMessage();
            if (msg instanceof BlockUpdate) {
                confirmations.addAll(handleBlockUpdate((BlockUpdate) msg, trie, mta));
            } else if (msg instanceof BlockProof) {
                confirmations.addAll(handleBlockProof((BlockProof) msg, mta));
            } else if (msg instanceof MessageProof) {
                msgs.addAll(handleMessageProof((MessageProof) msg, confirmations,
                            seq.add(BigInteger.valueOf(msgs.size())),
                            EthAddress.of(BTPAddress.parse(_prev).account()), BTPAddress.parse(_bmc)));
            }
        }

        this.tree.set(trie);
        this.mta.set(mta);

        int i = 0;
        byte[][]ret = new byte[msgs.size()][];
        for (MessageEvent msg : msgs) {
            ret[i++] = msg.getMessage();
        }
        return ret;
    }

    private List<Header> handleBlockUpdate(BlockUpdate bu, BlockTree trie, MerkleTreeAccumulator mta) {
        List<Header> newHeads = bu.getHeaders();
        if (newHeads.isEmpty()) {
            return new ArrayList<>();
        }

        Header parent = heads.get(newHeads.get(0).getParentHash());
        Context.require(parent != null, "Inconsistent block");

        for (Header head : newHeads) {
            verify(head, parent);
            trie.add(head);
            heads.set(head.getHash(), head);
            parent = head;
        }

        Hash newLeaf = newHeads.get(newHeads.size()-1).getHash();
        List<Header> confirmations = confirm(newLeaf);
        if (confirmations.size() > 0) {
            Hash newRoot = confirmations.get(0).getHash();
            // TODO improve to ensure that root snapshot exists
            snapshot(newRoot);
            trie.prune(newRoot, new BlockTree.OnRemoveListener() {
                @Override
                public void onRemove(Hash node) {
                    heads.set(node, null);
                    snapshots.set(node, null);
                }
            });
            for (int i = confirmations.size()-1; i>=0; i--) {
                mta.add(confirmations.get(i).getHash().toBytes());
            }
        }

        return confirmations;
    }

    private List<Header> handleBlockProof(BlockProof bp, MerkleTreeAccumulator mta) {
        Header head = bp.getHeader();
        if (head.getNumber().compareTo(BigInteger.valueOf(mta.getHeight())) > 0) {
            throw BMVException.unknown("Invalid block proof height");
        }

        try {
            mta.verify(bp.getWitness(), head.getHash().toBytes(),
                    head.getNumber().longValue()+1, bp.getHeight().intValue());
        } catch (Exception e) {
            throw BMVException.unknown(e.getMessage());
        }

        return new ArrayList<>() {{ add(head); }};
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
            for (EventLog log : receipt.getLogs()) {
                if (!log.getAddress().equals(prev)) {
                    continue;
                }

                MessageEvent msg = MessageEvent.of(bmc, log);
                if (!msg.getNext().equals(bmc)) {
                    continue;
                }

                if (msg.getSequence().compareTo(seq) != 0) {
                    BMVException.unknown("Invalid sequence");
                }

                seq = seq.add(BigInteger.ONE);
                msgs.add(msg);
            }
        }
        return msgs;
    }

    private void verify(Header head) {
        Context.require(head.getNumber().compareTo(BigInteger.ZERO) > 0, "Unknown block");
        Context.require(head.getUncleHash().equals(UNCLE_HASH), "Invalid uncle hash");
        if (head.getNumber().compareTo(BigInteger.ZERO) != 0) {
            Context.require(head.getDifficulty().compareTo(BigInteger.ZERO) != 0, "Invalid difficulty");
        }
        byte[] extra = head.getExtra();
        Context.require(extra.length >= EXTRA_VANITY, "Missing signer vanity");
        Context.require(extra.length >= EXTRA_VANITY + EXTRA_SEAL, "Missing signer seal");
        int signersBytes = extra.length - EXTRA_VANITY - EXTRA_SEAL;
        if (head.isEpochBlock()) {
            Context.require(signersBytes % EthAddress.ADDRESS_LEN == 0, "Malformed validators set bytes");
        } else {
            Context.require(signersBytes == 0, "Forbidden validators set bytes");
        }
        Context.require(head.getMixDigest().equals(Hash.EMPTY), "Invalid mix digest" + head.getMixDigest());
        Context.require(head.getGasLimit().compareTo(MIN_GAS_LIMIT) >= 0, "Invalid gas limit(< min)");
        Context.require(head.getGasLimit().compareTo(MAX_GAS_LIMIT) <= 0, "Invalid gas limit(> max)");
        Context.require(head.getGasUsed().compareTo(head.getGasLimit()) < 0, "Invalid gas used");
        Context.require(head.getSigner().equals(head.getCoinbase()), "Coinbase mismatch");
    }

    private void verify(Header head, Header parent) {
        verify(head);

        Context.require(Context.getBlockTimestamp() > head.getTime(), "Future block");
        verifyForkHashes(head);

        // verify cascading fields
        if (head.getNumber().equals(BigInteger.ZERO)) {
            return;
        }

        Context.require(parent.getNumber().add(BigInteger.ONE).equals(head.getNumber()),
                "Inconsistent block number");
        Context.require(parent.getHash().equals(head.getParentHash()), "Inconsistent block hash");

        Snapshot snap = snapshot(head.getParentHash());
        verifyForRamanujanFork(snap, head, parent);

        BigInteger diff = parent.getGasLimit().add(head.getGasLimit().negate());
        if (diff.compareTo(BigInteger.ZERO) < 0) {
            diff = diff.negate();
        }
        Context.require(diff.compareTo(parent.getGasLimit().divide(GAS_LIMIT_BOUND_DIVISOR)) < 0,
                "Invalid gas limit");

        Context.require(snap.getValidators().contains(head.getSigner()), "Unauthorized validator");
        Context.require(!snap.getRecents().contains(head.getSigner()) ||
                snap.getRecents().size() <= snap.getValidators().size() / 2 + 1, "Recently signed");
        if (snap.inturn(head.getSigner())) {
            Context.require(head.getDifficulty().equals(INTURN_DIFF), "Wrong difficulty(in-turn)");
        } else {
            Context.require(head.getDifficulty().equals(NOTURN_DIFF), "Wrong difficulty(no-turn)");
        }
    }

    private List<Header> confirm(Hash from) {
        Header head = heads.get(from);
        Snapshot snap = snapshot(head.getParentHash());
        List<Header> confirmations = new ArrayList<>();
        Map<EthAddress, Boolean> validators = new HashMap<>();
        while (snap != null) {
            EthAddresses newValidators = snap.getCandidates();
            validators.put(head.getSigner(), Boolean.TRUE);
            if (head.isEpochBlock()) {
                EthAddresses oldValidators = snap.getValidators();
                if (validators.size() > oldValidators.size() / 2 &&
                        countBy(validators, newValidators) > newValidators.size() * 2 / 3) {
                    confirmations.add(head);
                } else {
                    confirmations.clear();
                }
            } else if(!confirmations.isEmpty()) {
                confirmations.add(head);
            } else {
                if (countBy(validators, newValidators) > newValidators.size() * 2 / 3) {
                    confirmations.add(head);
                }
            }

            head = heads.get(head.getParentHash());
            snap = snapshot(head.getParentHash());
        }
        return confirmations;
    }

    private Snapshot snapshot(Hash id) {
        Snapshot snap;
        Header head = heads.get(id);
        if (head == null) {
            return null;
        }

        snap = snapshots.get(head.getHash());
        if (snap != null) {
            return snap;
        }

        List<Header> pendings = new ArrayList<>();
        while (head != null && snap == null) {
            pendings.add(head);
            snap = snapshots.get(head.getParentHash());
            head = heads.get(head.getParentHash());
        }

        for (int i = pendings.size() - 1; i >= 0; i--) {
            snap = snap.apply(pendings.get(i));
            snapshots.set(snap.getHash(), snap);
        }

        return snap;
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

    private void verifyForkHashes(Header head) {
        // TODO

        // BSC Mainnet: {
        //     EIP150_BLOCK: 0,
        //     EIP150_HASH: "0x0000000000000000000000000000000000000000000000000000000000000000"
        // }

        // if (head.getNumber().compareTo(Config.getAsBigInteger(Config.EIP150_BLOCK)) == 0) {
        //     Context.require(
        //             head.getHash().equals(new Hash(Config.getAsByteArray(Config.EIP150_HASH))),
        //             "homestead gas reprice fork");
        // }
    }

    private void verifyForRamanujanFork(Snapshot snap, Header head, Header parent) {
        // TODO

        // BSC Mainnet: {
        //     RAMANUJAN_BLOCK: 0,
        //     PERIOD: 3
        // }

        // if (head.getNumber().compareTo(Config.getAsBigInteger(Config.RAMANUJAN_BLOCK)) <= 0) {
        //     Context.require(head.getTime() >= parent.getTime() + Config.getAsBigInteger(Config.PERIOD).longValue() + backOffTime(snap, head.getCoinbase()), "Future block");
        // }
    }

}
