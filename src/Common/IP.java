package Common;

import java.util.ArrayList;
import java.util.List;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public abstract class IP {
    // #### ALL THE IP HANDLING CRAP ####

    public static int[] parseIP (String ip) throws IllegalArgumentException {
        if (ip == null) throw new IllegalArgumentException("can't parse null string");

        String[] parts = ip.split("\\."); // tokenize

        if (parts.length != 4) throw new IllegalArgumentException("IP addresses must be in x.x.x.x form");

        int[] ret = new int[4];

        for (int i = 0; i < 4; i++) {
            int val = -1;

            try {
                val = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid symbol in IP address. Use x.x.x.x with x in (0-255)");
            }

            if (val < 0 || val > 255) throw new IllegalArgumentException("Invalid IP addresses. Ensure they use x.x.x.x with x in (0-255)");

            ret[i] = val;
        }

        return ret;
    }

    public static boolean isConfigValid(int[] startIP, int[] endIP, int netMask, boolean showIP) {

        if (startIP.length != 4 || endIP.length != 4) {
            // sanity, should never happen
            Log.log(Log.LOG_ERROR, "IPs are not valid");
            return false;
        }

        int start = ipToInt(startIP);
        int end   = ipToInt(endIP);

        if (start > end) {
            Log.log(Log.LOG_ERROR, "The start IP must go BEFORE the end IP");
            return false;
        }

        if (netMask < 0 || netMask > 32) {
            Log.log(Log.LOG_ERROR, "Invalid netmask");
            return false;
        }

        int mask = netMask == 0 ? 0 : (~0 << (32 - netMask)); // create bitmask from slash number

        if ((start & mask) != (end & mask)) {
            Log.log(Log.LOG_ERROR, "IPs are not in the same subnet");
            return false;
        }

        int validIPs = 0;

        for (int ip = start; ip <= end; ip++) {
                if ((ip & mask) == ip) continue; // network
                if ((ip & ~mask) == ~mask) continue; // broadcast
            validIPs++;
            if (showIP) {
                System.out.println(intToIP(ip));
            }
        }

        if (validIPs == 0) {
            Log.log(Log.LOG_ERROR, "No valid IPs in range");
            return false;
        }

        if (showIP) {
            System.out.println("Total assignable IPs: " + validIPs);
        }

        return true;
    }

    public static String[] getValidIPs(int[] startIP, int[] endIP, int netMask) {
        if (startIP.length != 4 || endIP.length != 4 || netMask > 32 || netMask < 0) {
            return null;
        }

        int start = ((startIP[0] & 0xFF) << 24) |
                    ((startIP[1] & 0xFF) << 16) |
                    ((startIP[2] & 0xFF) << 8)  |
                    (startIP[3]  & 0xFF);

        int end   = ((endIP[0] & 0xFF) << 24) |
                    ((endIP[1] & 0xFF) << 16) |
                    ((endIP[2] & 0xFF) << 8)  |
                    (endIP[3]  & 0xFF);

        if (Integer.compareUnsigned(start, end) > 0) {
            return null;
        }

        int mask = netMask == 0 ? 0 : (~0 << (32 - netMask));

        List<String> validIPList = new ArrayList<>();

        for (long i = Integer.toUnsignedLong(start); i <= Integer.toUnsignedLong(end); i++) {
            int currentIP = (int) i;

            // ignore broadcast and network
            if ((currentIP & mask) == currentIP) continue;
            if ((currentIP & ~mask) == ~mask)    continue;

            String dottedIP = ((currentIP >>> 24) & 0xFF) + "." +
                              ((currentIP >>> 16) & 0xFF) + "." +
                              ((currentIP >>> 8)  & 0xFF) + "." +
                              (currentIP          & 0xFF);

            validIPList.add(dottedIP);
        }

        return validIPList.toArray(new String[0]);
    }

    public static int ipToInt(int[] ip) {
        return ((ip[0] & 0xFF) << 24) |
                ((ip[1] & 0xFF) << 16) |
                ((ip[2] & 0xFF) << 8)  |
                (ip[3] & 0xFF);
    }

    public static String intToIP(int ip) {
        return ((ip >> 24) & 0xFF) + "." +
            ((ip >> 16) & 0xFF) + "." +
            ((ip >> 8)  & 0xFF) + "." +
            (ip & 0xFF);
    }

    public static String intArrToIP(int[] ip) {
        return ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
    }

    public static boolean isBroadcast(int[] ip, int netMask) {
        int addr =  ((ip[0] & 0xFF) << 24) |
                    ((ip[1] & 0xFF) << 16) |
                    ((ip[2] & 0xFF) << 8)  |
                    (ip[3]  & 0xFF);

        int mask = netMask == 0 ? 0 : (~0 << (32 - netMask));

        return (addr & ~mask) == ~mask;
    }

    public static boolean isNetwork(int[] ip, int netMask) {
        int addr =  ((ip[0] & 0xFF) << 24) |
                    ((ip[1] & 0xFF) << 16) |
                    ((ip[2] & 0xFF) << 8)  |
                    (ip[3]  & 0xFF);

        int mask = netMask == 0 ? 0 : (~0 << (32 - netMask));

        return (addr & mask) == addr;
    }

    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // ignore ipv6
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.log(Log.LOG_ERROR, "Failed to enumerate network interfaces: " + e.getMessage());
        }

        return null; // Fallback if no valid IP is found
    }

    public static String getLocalIP(String targetInterface) {

        try {
            NetworkInterface netIf = NetworkInterface.getByName(targetInterface);

            if (netIf == null) {
                Log.log(Log.LOG_ERROR, "Network interface '" + targetInterface + "' does not exist.");
                return null;
            }

            if (!netIf.isUp()) {
                Log.log(Log.LOG_ERROR, "Network interface '" + targetInterface + "' is currently down.");
                return null;
            }

            Enumeration<InetAddress> addresses = netIf.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof java.net.Inet4Address) {
                    return addr.getHostAddress();
                }
            }
        } catch (java.net.SocketException e) {
            System.err.println("Error reading interface " + targetInterface + ": " + e.getMessage());
        }


        return null;
    }

}
