package foundation.icon.btp.bmv.bsc2;

public class Utils {

    public static int ceilDiv(int x, int y) {
        if (y == 0) {
            return 0;
        }
        return (x + y - 1) / y;
    }
}
