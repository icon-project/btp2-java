package foundation.icon.btp.bmv.bsc2;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class Validators {
    private List<Validator> validators;

    public Validators(List<Validator> validators) {
        this.validators = validators;
    }

    public static Validators fromBytes(byte[] bytes) {
        return Validators.readObject(Context.newByteArrayObjectReader("RLP", bytes));
    }

    public static Validators readObject(ObjectReader r) {
        r.beginList();
        List<Validator> validators = new ArrayList<>();
        while (r.hasNext()) {
            validators.add(r.read(Validator.class));
        }
        r.end();
        return new Validators(validators);
    }

    public static void writeObject(ObjectWriter w, Validators o) {
        w.beginList(o.validators.size());
        for (Validator validator : o.validators) {
            w.write(validator);
        }
        w.end();
    }

    public Validator get(int i) {
        return validators.get(i);
    }

    public EthAddresses getAddresses() {
        List<EthAddress> addresses = new ArrayList<>();
        for (Validator validator : validators) {
            addresses.add(validator.getAddress());
        }
        return new EthAddresses(addresses);
    }

    public List<BLSPublicKey> getPublicKeys() {
        List<BLSPublicKey> keys = new ArrayList<>();
        for (Validator validator : validators) {
            keys.add(validator.getPublicKey());
        }
        return keys;
    }

    public boolean contains(Validator validator) {
        return validators.contains(validator);
    }

    public int size() {
        return validators.size();
    }
}
