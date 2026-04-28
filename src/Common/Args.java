package Common;

public class Args {

    // All options that can be selected in the arguments
    public boolean forcePort = false;
    public int port = 67;

    public boolean sniffOnly = false;

    public boolean verbose = false; // mostly for more printing

    public boolean errorsFound = false; // if true, parsing is invalid, caller responsible for exit

    public boolean networkConfigChanged = false;
    public int[] ipStart = {192, 168, 1, 3};
    public int[] ipEnd   = {192, 168, 1, 254};
    public int[] gateway = {192, 168, 1, 1};
    public int[] dns     = {1, 1, 1, 1}; // cloudflare DNS

    public int netmask   = 24;

    public int leaseTime = 86400; // default 24h

    public String serverIP = IP.getLocalIP();

    public String targetInterface;

    public String servPxe;
    public int[] servPxeIP;
    public String filePxe;

    // constructor that parses the arguments
    public Args(String[] arguments) {

        boolean showIPPending = false; // show AFTER parsing the IPs and mask

        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];

            if (arg.equals("--help"))  {
                showHelp();
                errorsFound = true;
                break;
            }

            else if (arg.equals("--force-port")) {
                if (i+1 < arguments.length) {
                    if (!validateIntRange("--force-port", arguments[i+1], 1, 65535)) {
                        errorsFound = true;
                        break;
                    } else {
                        forcePort = true;
                        port = Integer.parseInt(arguments[i+1]);
                        i++;
                    }
                } else {
                    Log.log(Log.LOG_ERROR, "--force-port requires a value");
                    errorsFound = true;
                }
            }

            else if (arg.equals("--range")) {
                if (i + 2 >= arguments.length) {
                    Log.log(Log.LOG_ERROR, "--range requires two IPs provided");
                    errorsFound = true;
                    break;
                } else {
                    try {
                        ipStart = IP.parseIP(arguments[i+1]);
                        ipEnd   = IP.parseIP(arguments[i+2]);
                        i+=2;
                        networkConfigChanged = true;
                    } catch (IllegalArgumentException e) {
                        Log.log(Log.LOG_ERROR, "--range: " + e.getMessage());
                        errorsFound = true;
                    }
                }
            }

            else if (arg.equals("--mask")) {

                if (i+1 < arguments.length) {
                    if (!validateIntRange("--mask", arguments[i+1], 0, 32)) {
                        errorsFound = true;
                        break;
                    } else {
                        netmask = Integer.parseInt(arguments[i+1]);
                        networkConfigChanged = true;
                        i++;
                    }
                } else {
                    Log.log(Log.LOG_ERROR, "--mask requires a value");
                    errorsFound = true;
                    break;
                }


            }

            else if (arg.equals("--sniff-only"))  sniffOnly     = true;
            else if (arg.equals("--show-ip"))     showIPPending = true;

            else if (arg.equals("--gateway")) {
                arg = requireArg(arguments, i, "--gateway");
                if (arg == null) break;
                else {
                    try {
                        gateway = IP.parseIP(arg);
                        networkConfigChanged = true;
                        i++;
                    } catch (IllegalArgumentException e) {
                        Log.log(Log.LOG_ERROR, "--gateway: " + e.getMessage());
                        errorsFound = true;
                        break;
                    }
                }
            }

            else if (arg.equals("--dns")) {
                arg = requireArg(arguments, i, "--dns");
                if (arg == null) break;
                else {
                    try {
                        dns = IP.parseIP(arg);
                        networkConfigChanged = true;
                        i++;
                    } catch (IllegalArgumentException e) {
                        Log.log(Log.LOG_ERROR, "--dns: " + e.getMessage());
                        errorsFound = true;
                        break;
                    }
                }
            }

            else if (arg.equals("--lease-time")) {
                arg = requireArg(arguments, i, "--lease-time");
                if (arg == null) break;
                else {
                    if (!validateIntRange("--lease-time", arg, 0, Integer.MAX_VALUE)) {
                        errorsFound = true;
                        break;
                    } else {
                        leaseTime = Integer.parseInt(arg);
                        i++;
                    }
                }
            }

            else if (arg.equals("--force-server-ip")) {
                arg = requireArg(arguments, i, "--force-server-ip");
                if (arg == null) break;
                else {
                    try {
                        int[] numericServerIP = IP.parseIP(arg);
                        serverIP = IP.intArrToIP(numericServerIP);
                        networkConfigChanged = true;
                        i++;
                    } catch (IllegalArgumentException e) {
                        Log.log(Log.LOG_ERROR, "--force-server-ip: " + e.getMessage());
                        errorsFound = true;
                        break;
                    }
                }
            }

            else if (arg.equals("--interface")) {
                arg = requireArg(arguments, i, "--force-server-ip");
                if (arg == null) break;
                else {
                    targetInterface = arg;
                    serverIP = IP.getLocalIP(arg);
                    networkConfigChanged = true;
                    i++;
                }
            }

            // NOTE: it assumes everything is correct
            else if (arg.equals("--pxe")) {
                if (i + 2 >= arguments.length) {
                    Log.log(Log.LOG_ERROR, "--pxe requires TFTP server ip and filename");
                    errorsFound = true;
                    break;
                } else {
                    servPxe = arguments[i+1];
                    filePxe = arguments[i+2];
                    i+=2;
                    networkConfigChanged = true;

                    try {
                        servPxeIP = IP.parseIP(servPxe);
                    } catch (IllegalArgumentException e) {
                        Log.log(Log.LOG_WARNING, "Warning for PXE: address resolution not implemented. For non-UEFI systems, enter server IP");
                        servPxeIP = null;
                    }
                    
                    if (filePxe.length() > 128) {
                        Log.log(Log.LOG_WARNING, "Warning: PXE boot filename exceeds 128 chars. May not work on non-UEFI systems.");
                    }
                }
            }

            else if (arg.equals("--verbose"))     verbose       = true;

            // if the argument is invalid
            else {
                Log.log(Log.LOG_ERROR, "Unknown arguments");
                errorsFound = true;
                break;
            }
        }

        if (!errorsFound)
            if (!IP.isConfigValid(ipStart, ipEnd, netmask, showIPPending)) errorsFound = true;

        if (!networkConfigChanged && !errorsFound) {
            Log.log(Log.LOG_WARNING, "Warning: no parameters were set. Using defaults (see --help)");
        }

    }

    // used for mask and port
    public static boolean validateIntRange(String opt, String value, int min, int max) {
        int v;

        try {
            v = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Log.log(Log.LOG_ERROR, "Invalid number for " + opt);
            return false;
        }

        if (v < min || v > max) {
            Log.log(Log.LOG_ERROR, opt + " must be between " + min + "-" + max);
            return false;
        }

        return true;
    }

    private String requireArg(String[] args, int i, String opt) {
        if (i + 1 >= args.length) {
            Log.log(Log.LOG_ERROR, opt + " requires a value");
            errorsFound = true;
            return null;
        }
        return args[i + 1];
    }

    // #### USAGE AND PRINTING STUFF ####

    public static void showUsage() {
        System.out.println(
            "Usage:\n" +
            "[sudo] java NanoDHCP [ --help            | --force-port <n>        | --range <ip1> <ip2> |\n" +
            "                       --mask <m>        | --sniff-only            | --show-ip           |\n" +
            "                       --gateway <ip>    | --dns <ip>              | --lease-time <sec>  |\n" +
            "                       --interface <if>  | --force-server-ip <ip>  | --pxe <ip> <file>   |\n" +
            "                       --verbose         |                                                 ]\n"

        );

    }

    public static void showUsage(String msg) {
        System.out.println(msg);
        showUsage();
    }

    public void showSelectedOptions() { // mostly for debugging
    System.out.println(
        "==== PARSED OPTIONS ====\n" +

        "[General]\n" +
        "\tforce port     : " + forcePort + "\n" +
        "\tport           : " + port + "\n" +
        "\tsniff only     : " + sniffOnly + "\n" +
        "\tverbose        : " + verbose + "\n" +
        "\tlease time (s) : " + leaseTime + "\n" +

        "\n[Network]\n" +
        "\tip range       : " + IP.intArrToIP(ipStart) + " -> " + IP.intArrToIP(ipEnd) + "\n" +
        "\tnetmask        : /" + netmask + "\n" +
        "\tgateway        : " + IP.intArrToIP(gateway) + "\n" +
        "\tdns            : " + IP.intArrToIP(dns) + "\n" +
        "\tserver (local) : " + serverIP + "\n" +
        "\tinterface      : " + targetInterface + "\n" +
        "\tpxe server     : " + (servPxe != null ? servPxe : "disabled") + "\n" +
        "\tpxe file       : " + (filePxe != null ? filePxe : "disabled") + "\n" +

        "\n[Meta]\n" +
        "\tnetwork config changed : " + networkConfigChanged + "\n"
    );
}

    private void showHelp() {
        System.out.println(
            "NanoDHCP - tiny DHCP server in Java, built with love!\n\n" +

            "Options:\n\n" +

            "  --help                   Show this help message\n\n" +
            "  --force-port <port>      Bind socket to port (default: 67)\n" +
            "                           Required for real DHCP, otherwise useful for sniffing\n\n" +
            "  --range <ip1> <ip2>      IP pool range (default: 192.168.1.3 - 192.168.1.254)\n\n" +
            "  --mask <m>               Network mask in CIDR (default: 24)\n\n" +
            "  --gateway <ip>           Gateway IP to assign (default: 192.168.1.1)\n\n" +
            "  --dns <ip>               DNS server IP to assign (default: 1.1.1.1)\n\n" +
            "  --sniff-only             Do not respond, only listen to UDP traffic\n\n" +
            "  --show-ip                Print all valid IPs for current configuration\n\n" +
            "  --lease-time <sec>       Time to lease IPs for, in seconds (default: 24h)\n\n" +
            "  --force-server-ip <ip>   Forces packets to use a specific server IP (default: local)\n\n" +
            "  --interface <if>         Uses the server IP of the interface with name <if> (default: first found)\n\n" +
            "  --pxe <ip> <filename>    Tells PXE boot requests to use \"filename\" at the TFTP server with that ip\n\n" +
            "  --verbose                Enable verbose output\n"
        );
    }



}
