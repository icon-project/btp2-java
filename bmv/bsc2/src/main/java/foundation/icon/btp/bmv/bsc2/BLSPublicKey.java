package foundation.icon.btp.bmv.bsc2;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class BLSPublicKey {

    public static final int LENGTH = 48;
    private byte[] data;

    public BLSPublicKey(byte[] data) {
        Context.require(data.length == LENGTH, "Invalid bls public key");
        this.data = copy(data);
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
