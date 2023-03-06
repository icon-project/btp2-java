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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class Header {
    public static final int EXTRA_VANITY = 32;
    public static final int EXTRA_SEAL = 65;
    // pre-calculated constant uncle hash:) rlp([])
    public static final Hash UNCLE_HASH = Hash.of("1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347");
    public static final BigInteger INTURN_DIFF = BigInteger.valueOf(2L);
    public static final BigInteger NOTURN_DIFF = BigInteger.valueOf(1L);
    public static final BigInteger GAS_LIMIT_BOUND_DIVISOR = BigInteger.valueOf(256L);
    public static final BigInteger MAX_GAS_LIMIT = BigInteger.valueOf(0x7FFFFFFFFFFFFFFFL); // (2^63-1)
    public static final BigInteger MIN_GAS_LIMIT = BigInteger.valueOf(5000L);

    private Hash parentHash;
    private Hash uncleHash;
    private EthAddress coinbase;
    private Hash root;
    private Hash txHash;
    private Hash receiptHash;
    private byte[] bloom;
    private BigInteger difficulty;
    private BigInteger number;
    private BigInteger gasLimit;
    private BigInteger gasUsed;
    private long time;
    private byte[] extra;
    private Hash mixDigest;
    private byte[] nonce;

    // caches
    private EthAddress signerCache;
    private List<EthAddress> validatorsCache;
    private Hash hashCache;

    public Header(Hash parentHash, Hash uncleHash, EthAddress coinbase, Hash root,
            Hash txHash, Hash receiptHash, byte[] bloom, BigInteger difficulty,
            BigInteger number, BigInteger gasLimit, BigInteger gasUsed, long time,
            byte[] extra, Hash mixDigest, byte[] nonce)
    {
        this.parentHash = parentHash;
        this.uncleHash = uncleHash;
        this.coinbase = coinbase;
        this.root = root;
        this.txHash = txHash;
        this.receiptHash = receiptHash;
        this.bloom = bloom;
        this.difficulty = difficulty;
        this.number = number;
        this.gasLimit = gasLimit;
        this.gasUsed = gasUsed;
        this.time = time;
        this.extra = extra;
        this.mixDigest = mixDigest;
        this.nonce = nonce;
    }

    public static Header readObject(ObjectReader r) {
        r.beginList();
        Hash parentHash = r.read(Hash.class);
        Hash uncleHash = r.read(Hash.class);
        EthAddress coinbase = r.read(EthAddress.class);
        Hash root = r.read(Hash.class);
        Hash txHash = r.read(Hash.class);
        Hash receiptHash = r.read(Hash.class);
        byte[] bloom = r.readByteArray();
        BigInteger difficulty = r.readBigInteger();
        BigInteger number = r.readBigInteger();
        BigInteger gasLimit = r.readBigInteger();
        BigInteger gasUsed = r.readBigInteger();
        long time = r.readLong();
        byte[] extra = r.readByteArray();
        Hash mixDigest = r.read(Hash.class);
        byte[] nonce = r.readByteArray();
        r.end();
        return new Header(parentHash, uncleHash, coinbase, root, txHash, receiptHash, bloom,
                difficulty, number, gasLimit, gasUsed, time, extra, mixDigest, nonce);
    }

    public static void writeObject(ObjectWriter w, Header o) {
        w.beginList(15);
        w.writeNullable(o.parentHash);
        w.writeNullable(o.uncleHash);
        w.writeNullable(o.coinbase);
        w.writeNullable(o.root);
        w.writeNullable(o.txHash);
        w.writeNullable(o.receiptHash);
        w.write(o.bloom);
        w.writeNullable(o.difficulty);
        w.writeNullable(o.number);
        w.writeNullable(o.gasLimit);
        w.writeNullable(o.gasUsed);
        w.write(o.time);
        w.write(o.extra);
        w.writeNullable(o.mixDigest);
        w.write(o.nonce);
        w.end();
    }

    public static Header fromBytes(byte[] bytes) {
        ObjectReader r = Context.newByteArrayObjectReader("RLP", bytes);
        return Header.readObject(r);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLP");
        writeObject(w, this);
        return w.toByteArray();
    }

    public boolean isEpochBlock() {
        return number.mod(Config.getAsBigInteger(Config.EPOCH)) == BigInteger.ZERO;
    }

    public Hash getHash() {
        if (hashCache == null) {
            hashCache = Hash.of(Context.hash("keccak-256", toBytes()));
        }
        return hashCache;
    }

    public List<EthAddress> getValidators() {
        Context.require(isEpochBlock(), "Not epoch block");
        if (validatorsCache == null) {
            byte[] signersBytes = Arrays.copyOfRange(extra, EXTRA_VANITY, extra.length - EXTRA_SEAL);
            int n = signersBytes.length / EthAddress.ADDRESS_LEN;
            validatorsCache = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                validatorsCache.add(new EthAddress(Arrays.copyOfRange(signersBytes, i * EthAddress.ADDRESS_LEN, (i+1) * EthAddress.ADDRESS_LEN)));
            }
        }
        return validatorsCache;
    }

    public EthAddress getSigner() {
        if (signerCache == null) {
            assert(extra.length >= EXTRA_SEAL);
            byte[] signature = Arrays.copyOfRange(extra, extra.length - EXTRA_SEAL, extra.length);
            byte[] pubkey = Context.recoverKey("ecdsa-secp256k1", getSealHash(), signature, false);
            byte[] pubhash  = Context.hash("keccak-256", Arrays.copyOfRange(pubkey, 1, pubkey.length));
            signerCache = new EthAddress(Arrays.copyOfRange(pubhash, 12, pubhash.length));
        }
        return signerCache;
    }

    private byte[] getSealHash() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLP");
        w.beginList(16);
        w.writeNullable(Config.getAsBigInteger(Config.CHAIN_ID));
        w.writeNullable(parentHash);
        w.writeNullable(uncleHash);
        w.writeNullable(coinbase);
        w.writeNullable(root);
        w.writeNullable(txHash);
        w.writeNullable(receiptHash);
        w.write(bloom);
        w.writeNullable(difficulty);
        w.writeNullable(number);
        w.writeNullable(gasLimit);
        w.writeNullable(gasUsed);
        w.write(time);
        w.write(Arrays.copyOfRange(extra, 0, extra.length-65));
        w.writeNullable(mixDigest);
        w.write(nonce);
        w.end();
        return Context.hash("keccak-256", w.toByteArray());
    }

    public Hash getRoot() {
        return root;
    }

    public Hash getTxHash() {
        return txHash;
    }

    public Hash getReceiptHash() {
        return receiptHash;
    }

    public byte[] getBloom() {
        return bloom;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public Hash getParentHash() {
        return this.parentHash;
    }

    public Hash getUncleHash() {
        return this.uncleHash;
    }

    public EthAddress getCoinbase() {
        return this.coinbase;
    }

    public BigInteger getDifficulty() {
        return this.difficulty;
    }

    public BigInteger getGasLimit() {
        return this.gasLimit;
    }

    public BigInteger getGasUsed() {
        return this.gasUsed;
    }

    public long getTime() {
        return this.time;
    }

    public BigInteger getNumber() {
        return this.number;
    }

    public byte[] getExtra() {
        return this.extra;
    }

    public Hash getMixDigest() {
        return this.mixDigest;
    }

}
