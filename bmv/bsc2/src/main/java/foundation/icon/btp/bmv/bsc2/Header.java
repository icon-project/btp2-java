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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import foundation.icon.score.util.StringUtil;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class Header {
    public static final int EXTRA_VANITY = 32;
    public static final int VALIDATOR_NUMBER_SIZE = 1;
    public static final int VALIDATOR_BYTES_LENGTH = EthAddress.LENGTH + BLSPublicKey.LENGTH;
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
    private Hash hashCache;
    private Validators valsCache;
    private VoteAttestation atteCache;

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
        w.write(o.parentHash);
        w.write(o.uncleHash);
        w.write(o.coinbase);
        w.write(o.root);
        w.write(o.txHash);
        w.write(o.receiptHash);
        w.write(o.bloom);
        w.write(o.difficulty);
        w.write(o.number);
        w.write(o.gasLimit);
        w.write(o.gasUsed);
        w.write(o.time);
        w.write(o.extra);
        w.write(o.mixDigest);
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

    public Hash getHash() {
        if (hashCache == null) {
            hashCache = Hash.of(Context.hash("keccak-256", toBytes()));
        }
        return hashCache;
    }

    public Validators getValidators(ChainConfig config) {
        if (valsCache == null) {
            List<Validator> validators = new ArrayList<>();
            byte[] b = getValidatorBytes(config);
            int n = b.length / VALIDATOR_BYTES_LENGTH;
            for (int i = 0; i < n; i++) {
                byte[] consensus = Arrays.copyOfRange(b, i * VALIDATOR_BYTES_LENGTH,
                        i * VALIDATOR_BYTES_LENGTH + EthAddress.LENGTH);
                byte[] vote = Arrays.copyOfRange(b, i * VALIDATOR_BYTES_LENGTH + EthAddress.LENGTH,
                        (i + 1) * VALIDATOR_BYTES_LENGTH);
                validators.add(new Validator(new EthAddress(consensus), new BLSPublicKey(vote)));
            }
            valsCache = new Validators(validators);
        }
        return valsCache;
    }

    public byte[] getValidatorBytes(ChainConfig config) {
        if (extra.length <= EXTRA_VANITY + EXTRA_SEAL) {
            return null;
        }

        if (!config.isEpoch(number)) {
            return null;
        }
        int num = extra[EXTRA_VANITY];
        if (num == 0 || extra.length <= EXTRA_VANITY + EXTRA_SEAL + num * VALIDATOR_BYTES_LENGTH) {
            return null;
        }

        int start = EXTRA_VANITY + VALIDATOR_NUMBER_SIZE;
        int end = start + num * VALIDATOR_BYTES_LENGTH;
        return Arrays.copyOfRange(extra, start, end);
    }

    public VoteAttestation getVoteAttestation(ChainConfig config) {
        if (extra.length <= EXTRA_VANITY + EXTRA_SEAL) {
            return null;
        }

        if (atteCache == null) {
            byte[] blob;
            if (!config.isEpoch(number)) {
                blob = Arrays.copyOfRange(extra, EXTRA_VANITY, extra.length - EXTRA_SEAL);
            } else {
                int num = extra[EXTRA_VANITY];
                if (extra.length <= EXTRA_VANITY + EXTRA_SEAL + VALIDATOR_NUMBER_SIZE + num * VALIDATOR_BYTES_LENGTH) {
                    return null;
                }
                int start = EXTRA_VANITY + VALIDATOR_NUMBER_SIZE + num * VALIDATOR_BYTES_LENGTH;
                int end = extra.length - EXTRA_SEAL;
                blob = Arrays.copyOfRange(extra, start, end);
            }
            atteCache = VoteAttestation.fromBytes(blob);
        }
        return atteCache;
    }

    public EthAddress getSigner(BigInteger cid) {
        Context.require(extra.length >= EXTRA_SEAL, "Invalid seal bytes");
        byte[] signature = Arrays.copyOfRange(extra, extra.length - EXTRA_SEAL, extra.length);
        byte[] pubkey = Context.recoverKey("ecdsa-secp256k1", getSealHash(cid), signature, false);
        byte[] pubhash  = Context.hash("keccak-256", Arrays.copyOfRange(pubkey, 1, pubkey.length));
        return new EthAddress(Arrays.copyOfRange(pubhash, 12, pubhash.length));
    }

    private byte[] getSealHash(BigInteger cid) {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLP");
        w.beginList(16);
        w.write(cid);
        w.write(parentHash);
        w.write(uncleHash);
        w.write(coinbase);
        w.write(root);
        w.write(txHash);
        w.write(receiptHash);
        w.write(bloom);
        w.write(difficulty);
        w.write(number);
        w.write(gasLimit);
        w.write(gasUsed);
        w.write(time);
        w.write(Arrays.copyOfRange(extra, 0, extra.length-65));
        w.write(mixDigest);
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

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Header)) {
            return false;
        }
        Header other = (Header) o;
        return this.getHash().equals(other.getHash());
    }

}
