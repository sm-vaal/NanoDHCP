package Common;

public abstract class ByteFuncts {
    public static String toHexString(int x) {
        return String.format("%02X %02X %02X %02X",
            (x >>> 24) & 0xFF,
            (x >>> 16) & 0xFF,
            (x >>> 8 ) & 0xFF,
             x         & 0xFF
        );
    }
}
