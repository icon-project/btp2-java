package foundation.icon.btp.bmv.bsc2;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class VoteAttestation {
    public static final int BLS_SIG_LENGTH = 96;
    private static final int VOTES_BYTES_LENGTH = 8;
    private final byte[] votes;
    private final byte[] signature;
    private final VoteRange range;
    // reserved
    private final byte[] extra;

    public VoteAttestation(byte[] votes, byte[] signature, VoteRange range, byte[] extra) {
        this.votes = Utils.copy(votes);
        this.signature = Utils.copy(signature);
        this.range = range;
        this.extra = Utils.copy(extra);
    }

    public static VoteAttestation fromBytes(byte[] bytes) {
        return VoteAttestation.readObject(Context.newByteArrayObjectReader("RLP", bytes));
    }

    public static VoteAttestation readObject(ObjectReader r) {
        r.beginList();
        byte[] votes = r.readByteArray();
        byte[] signature = r.readByteArray();
        Context.require(signature.length == BLS_SIG_LENGTH, "Invalid signature");
        VoteRange range = r.read(VoteRange.class);
        byte[] extra = r.readByteArray();
        r.end();
        return new VoteAttestation(votes, signature, range, extra);
    }

    public static void writeObject(ObjectWriter w, VoteAttestation o) {
        w.beginList(4);
        w.write(o.votes);
        w.write(o.signature);
        w.write(o.range);
        w.write(o.extra);
        w.end();
    }

    public VoteRange getVoteRange() {
        return range;
    }

    public boolean isSourceOf(BigInteger number, Hash hash) {
        return range.getSourceNumber().compareTo(number) == 0 && range.getSourceHash().equals(hash);
    }

    public boolean isTargetOf(BigInteger number, Hash hash) {
        return range.getTargetNumber().compareTo(number) == 0 && range.getTargetHash().equals(hash);
    }

    public void verify(Validators validators) {
        byte[] aggr = aggregate(validators);
        Context.verifySignature("bls12-381-g2", range.hash(), signature, aggr);
    }

    public byte[] aggregate(Validators validators) {
        Context.require(count() <= validators.size(), "Invalid vote - larger than validators");

        List<BLSPublicKey> keys = new ArrayList<>();
        for (int i = 0; i < validators.size(); i++) {
            if (!mask(i)) {
                continue;
            }
            keys.add(validators.get(i).getPublicKey());
        }
        Context.require(keys.size() >= Utils.ceilDiv(validators.size() * 2, 3), "Short quorum");

        byte[] aggregation = null;
        for (BLSPublicKey key : keys) {
            aggregation = Context.aggregate("bls12-381-g1", aggregation, key.toBytes());
        }
        return aggregation;
    }

    private int count() {
        int cnt = 0;
        for (byte bitset : this.votes) {
            for (int j = 0; j < 8; j++) {
                if ((bitset >> j & 1) == 1) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    private boolean mask(int nth) {
        Context.require(nth <= 64, "Invalid voter index");
        int i = nth / 8;
        Context.require(i < VOTES_BYTES_LENGTH, "Invalid bitset access");
        return (votes[i] >> (nth % 8) & 1) == 1;
    }
}
