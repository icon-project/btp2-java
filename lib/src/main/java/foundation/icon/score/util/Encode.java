package foundation.icon.score.util;

import score.ByteArrayObjectWriter;
import score.Context;

import java.math.BigInteger;

public class Encode {
    public static byte[] encode(BigInteger[] arr) {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        w.beginList(arr.length);
        for (BigInteger v : arr) {
            w.write(v);
        }
        w.end();
        return w.toByteArray();
    }

    public static byte[] encode(BigInteger[][] twoDimArr) {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        w.beginList(twoDimArr.length);
        for (BigInteger[] arr : twoDimArr) {
            w.beginList(arr.length);
            for (BigInteger v : arr) {
                w.write(v);
            }
            w.end();
        }
        w.end();
        return w.toByteArray();
    }

    public static byte[] encode(String[] arr) {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        w.beginList(arr.length);
        for (String v : arr) {
            w.write(v);
        }
        w.end();
        return w.toByteArray();
    }

}
