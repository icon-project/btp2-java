package foundation.icon.btp.bmv.bsc2;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class VoteRange {

    public final BigInteger sourceNumber;
    public final Hash sourceHash;
    public final BigInteger targetNumber;
    public final Hash targetHash;

    public static VoteRange readObject(ObjectReader r) {
        r.beginList();
        BigInteger sn = r.readBigInteger();
        Hash sh = r.read(Hash.class);
        BigInteger tn = r.readBigInteger();
        Hash th = r.read(Hash.class);
        r.end();
        return new VoteRange(sn, sh, tn, th);
    }

    public static void writeObject(ObjectWriter w, VoteRange o) {
        w.beginList(4);
        w.write(o.sourceNumber);
        w.write(o.sourceHash);
        w.write(o.targetNumber);
        w.write(o.targetHash);
        w.end();
    }

    public VoteRange(BigInteger sn, Hash sh, BigInteger tn, Hash th) {
        this.sourceNumber = sn;
        this.sourceHash = sh;
        this.targetNumber = tn;
        this.targetHash = th;
    }

    public BigInteger getSourceNumber() {
        return sourceNumber;
    }

    public Hash getSourceHash() {
        return sourceHash;
    }

    public BigInteger getTargetNumber() {
        return targetNumber;
    }

    public Hash getTargetHash() {
        return targetHash;
    }

    public byte[] hash() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLP");
        writeObject(w, this);
        return Context.hash("keccak-256", w.toByteArray());
    }

}
