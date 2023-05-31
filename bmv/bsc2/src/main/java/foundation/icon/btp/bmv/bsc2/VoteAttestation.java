package foundation.icon.btp.bmv.bsc2;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

import static foundation.icon.btp.bmv.bsc2.BLSPublicKey.LENGTH;

public class VoteAttestation {

    private final String ERR_INVALID_VOTER_COUNT = "Invalid attestation, vote number larger than validators number";
    private final String ERR_SHORT_QUORUM = "Invalid attestation, not enough votes";

    public static final int MAX_EXTRA_LENGTH = 256;
    public static final int BLS_SIG_LENGTH = 96;
    private final Voters voters;
    private final byte[] signature;
    private final VoteRange range;
    // reserved
    private final byte[] extra;

    public VoteAttestation(Voters voters, byte[] signature, VoteRange range, byte[] extra) {
        this.voters = voters;
        this.signature = Utils.copy(signature);
        this.range = range;
        this.extra = Utils.copy(extra);
    }

    public static VoteAttestation fromBytes(byte[] bytes) {
        return VoteAttestation.readObject(Context.newByteArrayObjectReader("RLP", bytes));
    }

    public static VoteAttestation readObject(ObjectReader r) {
        r.beginList();
        byte[] bitset = new byte[1];
        bitset[0] = r.readByte();
        Voters voters = new Voters(bitset);
        // TODO
                //r.read(Voters.class);
        //new Voters(r.readByteArray());
        byte[] signature = r.readByteArray();
        Context.require(signature.length == BLS_SIG_LENGTH, "Invalid signature");
        VoteRange range = r.read(VoteRange.class);
        byte[] extra = r.readByteArray();
        r.end();
        return new VoteAttestation(voters, signature, range, extra);
    }

    public static void writeObject(ObjectWriter w, VoteAttestation o) {
        w.beginList(4);
        // TODO
        //o.voters.
        w.write(o.voters.bitset[0]);
        w.write(o.signature);
        w.write(o.range);
        w.write(o.extra);
        w.end();
    }

    public VoteRange getVoteRange() {
        return range;
    }

    // TODO
    // public byte[] getExtra() {
    //     return Utils.copy(this.extra);
    // }

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
        Context.require(voters.count() <= validators.size(), ERR_INVALID_VOTER_COUNT);

        List<BLSPublicKey> keys = new ArrayList<>();
        for (int i = 0; i < validators.size(); i++) {
            if (!voters.contains(i)) {
                continue;
            }
            keys.add(validators.get(i).getPublicKey());
        }
        Context.require(keys.size() >= Utils.ceilDiv(validators.size() * 2, 3), ERR_SHORT_QUORUM);

        byte[] aggregation = null;
        for (int i = 0; i < keys.size(); i++) {
            aggregation = Context.aggregate("bls12-381-g1", aggregation, keys.get(i).toBytes());
        }
        return aggregation;
    }

}
