package foundation.icon.btp.bmv.bsc2;

import score.ObjectReader;
import score.ObjectWriter;

public class Validator {
    private final EthAddress address;
    private final BLSPublicKey pubkey;

    public Validator(EthAddress address, BLSPublicKey pubkey) {
        this.address = address;
        this.pubkey = pubkey;
    }

    public static Validator readObject(ObjectReader r) {
        r.beginList();
        EthAddress address = new EthAddress(r.readByteArray());
        BLSPublicKey pubkey = new BLSPublicKey(r.readByteArray());
        r.end();
        return new Validator(address, pubkey);
    }

    public static void writeObject(ObjectWriter w, Validator o) {
        w.beginList(2);
        w.write(o.address);
        w.write(o.pubkey);
        w.end();
    }

    public EthAddress getAddress() {
        return address;
    }

    public BLSPublicKey getPublicKey() {
        return pubkey;
    }
}
