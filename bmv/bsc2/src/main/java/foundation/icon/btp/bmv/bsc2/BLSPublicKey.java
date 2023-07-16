package foundation.icon.btp.bmv.bsc2;

import foundation.icon.score.util.StringUtil;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.util.Arrays;

public class BLSPublicKey {

    public static final int LENGTH = 48;
    private final byte[] data;

    public BLSPublicKey(byte[] data) {
        Context.require(data.length == LENGTH, "Invalid bls public key");
        this.data = copy(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BLSPublicKey that = (BLSPublicKey) o;
        return Arrays.equals(data, that.data);
    }

    @Override
    public String toString() {
        return StringUtil.toString(data);
    }

    public static BLSPublicKey readObject(ObjectReader r) {
        return new BLSPublicKey(r.readByteArray());
    }

    public static void writeObject(ObjectWriter w, BLSPublicKey o) {
        w.write(o.data);
    }

    public byte[] toBytes() {
        return copy(this.data);
    }

    private static byte[] copy(byte[] src) {
        byte[] dst = new byte[LENGTH];
        int i = -1;
        while (++i < LENGTH) {
            dst[i] = src[i];
        }
        return dst;
    }
}
