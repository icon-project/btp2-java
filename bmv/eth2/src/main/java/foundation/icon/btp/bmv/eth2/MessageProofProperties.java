package foundation.icon.btp.bmv.eth2;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class MessageProofProperties {
    private byte[] ethBmc;
    private BigInteger lastMsgSeq;
    private BigInteger lastMsgSlot;
    public static final MessageProofProperties DEFAULT;

    static {
        DEFAULT = new MessageProofProperties();
    }

    public byte[] getEthBmc() {
        return ethBmc;
    }

    public void setEthBmc(byte[] ethBmc) {
        this.ethBmc = ethBmc;
    }

    public BigInteger getLastMsgSeq() {
        return lastMsgSeq;
    }

    public void setLastMsgSeq(BigInteger lastMsgSeq) {
        this.lastMsgSeq = lastMsgSeq;
    }

    public BigInteger getLastMsgSlot() {
        return lastMsgSlot;
    }

    public void setLastMsgSlot(BigInteger lastMsgSlot) {
        this.lastMsgSlot = lastMsgSlot;
    }

    public static MessageProofProperties readObject(ObjectReader r) {
        r.beginList();
        var object = new MessageProofProperties();
        object.setEthBmc(r.readByteArray());
        object.setLastMsgSeq(r.readBigInteger());
        object.setLastMsgSlot(r.readBigInteger());
        r.end();
        return object;
    }

    public static void writeObject(ObjectWriter w, MessageProofProperties obj) {
        w.beginList(3);
        w.write(obj.ethBmc);
        w.write(obj.lastMsgSeq);
        w.write(obj.lastMsgSlot);
        w.end();
    }
}
