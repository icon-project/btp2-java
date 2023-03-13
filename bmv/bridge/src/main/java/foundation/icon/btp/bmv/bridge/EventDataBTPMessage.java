package foundation.icon.btp.bmv.bridge;

import foundation.icon.score.util.StringUtil;
import score.ObjectReader;

import java.math.BigInteger;

public class EventDataBTPMessage {

    final static String RLPn = "RLPn";
    private final byte[] next_bmc;
    private final BigInteger seq;
    private final byte[] msg;

    public EventDataBTPMessage(byte[] next_bmc, BigInteger seq, byte[] msg) {
        this.next_bmc = next_bmc;
        this.seq = seq;
        this.msg = msg;
    }

    /**
     * Method to extract raw data directly from the reader without the TypeDecoder
     * @param reader
     * @return
     */
    public static EventDataBTPMessage fromRLPBytes(ObjectReader reader) {
        reader.beginList();
        byte[] _nxt_bmc = reader.readByteArray();
        BigInteger _seq = reader.readBigInteger();
        byte[] _msg = reader.readByteArray();
        reader.end();
        return new EventDataBTPMessage(_nxt_bmc, _seq, _msg);
    }

    public byte[] getNext_bmc() {
        return next_bmc;
    }

    public BigInteger getSeq() {
        return seq;
    }

    public byte[] getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EventDataBTPMessage{");
        sb.append("next_bmc='").append(StringUtil.toString(next_bmc)).append('\'');
        sb.append(", seq=").append(seq);
        sb.append(", msg=").append(StringUtil.toString(msg));
        sb.append('}');
        return sb.toString();
    }
}
