package foundation.icon.btp.bmv.bsc2;

import score.Context;

public class BLSSignature {
    private byte[] sig;

    public BLSSignature(byte[] sig) {
        Context.require(sig.length == 96, "Invalid bls signature length");
        this.sig = new byte[96];
        System.arraycopy(sig, 0, this.sig, 0, 96);
    }

    public void verify(byte[] msg, byte[] aggr) {
        Context.verifySignature("bls12-381-g2", msg, sig, aggr);
    }
}
