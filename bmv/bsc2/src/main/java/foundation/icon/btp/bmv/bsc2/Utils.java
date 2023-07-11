package foundation.icon.btp.bmv.bsc2;

public class Utils {

    public static byte[] copy(byte[] src) {
        byte[] dst = new byte[src.length];
        int i = -1;
        while (++i < src.length) {
            dst[i] = src[i];
        }
        return dst;
    }

    public static int ceilDiv(int x, int y) {
        if (y == 0) {
            return 0;
        }
        return (x + y - 1) / y;
    }
}
