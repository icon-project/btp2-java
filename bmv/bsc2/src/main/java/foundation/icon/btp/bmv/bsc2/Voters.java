package foundation.icon.btp.bmv.bsc2;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class Voters {

    public static final int BITSET_BYTES_LENGTH = 8;
    public byte[] bitset;

    public Voters(byte[] bitset) {
        Context.require(bitset.length <= BITSET_BYTES_LENGTH, "Invalid bitset size");
        this.bitset = new byte[8];
        System.arraycopy(bitset, 0, this.bitset, 0, bitset.length);
    }

    public int count() {
        int cnt = 0;
        for (byte b : bitset) {
            for (int j = 0; j < 8; j++) {
                if ((b >> j & 1) == 1) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    public boolean contains(int v) {
        Context.require(v <= 64, "Invalid voter index");
        int index = v / 8;
        Context.require(index < BITSET_BYTES_LENGTH, "Invalid bitset access");
        return (bitset[index] >> (v % 8) & 1) == 1;
    }

    public static Voters readObject(ObjectReader r) {
        r.beginList();
        byte[] bitset = r.readByteArray();
        r.end();
        return new Voters(bitset);
    }

    public static void writeObject(ObjectWriter w, Voters o) {
        w.beginList(1);
        w.write(o.bitset);
        w.end();
    }
}
