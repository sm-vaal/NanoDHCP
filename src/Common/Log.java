package Common;

public class Log {
    // I'm aware of existing stuff for this. A bit useless indeed

    public static int LOG_ALL     = 4; // all includes basically all info
    public static int LOG_INFO    = 3;
    public static int LOG_WARNING = 2;
    public static int LOG_ERROR   = 1;
    public static int LOG_NONE    = 0;

    private static int logLevel = 1; // only errors by default printed

    public static void setLogLevel(int level) {
        if (level > LOG_ALL) {
            System.out.println("\tDEVELOPER!!!: Invalid error log level, clamped down to max");
            level = LOG_ALL;
        } else if (level < LOG_NONE) {
            System.out.println("\tDEVELOPER!!!: Invalid error log level, clamped up to min");
            level = LOG_NONE;
        }

        logLevel = level;
    }

    public static void log(int level, String msg) {
        if (level > LOG_ALL) {
            System.out.println("\tDEVELOPER!!!: Invalid error log level, clamped down to max");
            level = LOG_ALL;
        } else if (level < LOG_NONE) {
            System.out.println("\tDEVELOPER!!!: Invalid error log level, clamped up to min");
            level = LOG_NONE;
        }

        if (level <= logLevel) {
            System.out.println(msg);
        }
    }

}
