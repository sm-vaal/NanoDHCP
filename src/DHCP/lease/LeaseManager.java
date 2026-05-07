package DHCP.lease;

import Common.*;

import java.util.Map;
import java.util.HashMap;

public class LeaseManager {

    int startIP;
    int endIP;
    int netmask;

    // we follow simple round robin: always assign the next available if not in mac map
    int currentIPIndex = 0;

    String[] validIPs; // useful for printing

    private Map<String, Lease> activeLeasesMac = new HashMap<String, Lease>();
    private Map<String, Lease> activeLeasesIP  = new HashMap<String, Lease>();

    public LeaseManager(int[] startIP, int[] endIP, int netmask) {
        if (!IP.isConfigValid(startIP, endIP, netmask, false)) {
            Log.log(Log.LOG_ERROR, "LeaseManager could not be created: invalid config");
            throw new IllegalArgumentException("invalid IP config for LeaseManager");
        }

        validIPs       = IP.getValidIPs(startIP, endIP, netmask);
        this.startIP   = IP.ipToInt(startIP);
        this.endIP     = IP.ipToInt(endIP);
        this.netmask   = netmask;


        Log.log(Log.LOG_INFO, "LeaseManager created succesfully");
    }

    public Lease getLeaseByMac(String macAddress) {
        return activeLeasesMac.get(macAddress);
    }

    public Lease getLeaseByIP(String ip) {
        return activeLeasesIP.get(ip);
    }

    public Lease getLeaseByMac(byte[] macAddress) {
        if (macAddress.length < 6) {
            Log.log(Log.LOG_ERROR, "mac address too short to get lease: " + macAddress.length + "bytes");
            return null;
        } else if (macAddress.length > 6) {
            Log.log(Log.LOG_WARNING, "mac address too long to get lease: " + macAddress.length + " bytes");
            return null;
        }
        return this.getLeaseByMac(macToString(macAddress));
    }

    public Lease getLeaseByIP(int[] ip) {
        if (ip.length < 4) {
            Log.log(Log.LOG_ERROR, "IP address too short to get lease: " + ip.length + "bytes");
            return null;
        } else if (ip.length > 4) {
            Log.log(Log.LOG_WARNING, "IP address too long to get lease: " + ip.length + " bytes");
            return null;
        }
        return this.getLeaseByIP(IP.intArrToIP(ip));
    }

    // returns null if couldn't find
    public String findFirstIPAvailable() {
        boolean assigned = false;
        int tried = 0;
        Lease l;

        // try all IPs. Lazy expiration
        while (tried != validIPs.length && !assigned) {
            String currIP = validIPs[currentIPIndex];

            if (currIP.equals(IP.getLocalIP())) {
                currentIPIndex++;
                tried++;
                continue;
            }

            l = activeLeasesIP.get(validIPs[currentIPIndex]); 

            if (l == null) { // if null, never assigned
                assigned = true;
                break;
            }

            assigned = l.isAvailable() || l.isExpired();
            if (assigned) break;

            tried++;
            currentIPIndex++;
            if (currentIPIndex >= validIPs.length) currentIPIndex = 0;
        }

        if (!assigned) {
            Log.log(Log.LOG_WARNING, "Could not find an IP, all in pool are assigned");
            return null;
        } else {
            Log.log(Log.LOG_INFO, "Free IP found: " + validIPs[currentIPIndex]);
            return validIPs[currentIPIndex];
        }
    }

    // null if not assigned, the ip if assigned
    // meant to be used after a DHCPDISCOVER, as a DHCPOFFER
    public Lease assignToFirstLease(String macAddress, int timeSeconds, int transID) {
        String ip = findFirstIPAvailable();
        if (ip == null) return null;

        // cleanup if ip belonged to someone
        Lease oldLease = activeLeasesIP.get(ip);
        if (oldLease != null && !oldLease.macAddress.equals(macAddress)) {
            activeLeasesMac.remove(oldLease.macAddress);
            Log.log(Log.LOG_INFO, "Cleaned up stale MAC mapping for " + oldLease.macAddress);
        }

        Lease newLease = new Lease(ip, macAddress, Lease.LeaseState.OFFERED,
                                   timeSeconds, transID);

        // create new lease for this ip.
        activeLeasesIP.put(ip, newLease);
        activeLeasesMac.put(macAddress, newLease);

        Log.log(Log.LOG_INFO, "IP " + ip + " set to offered for MAC " + macAddress);

        return newLease;
    }

    public Lease assignToLease(String ip, String macAddress, int timeSeconds, int transID, Lease.LeaseState state) {
        if (ip == null) return null;

        // cleanup if ip belonged to someone
        Lease oldLease = activeLeasesIP.get(ip);
        if (oldLease != null && !oldLease.macAddress.equals(macAddress)) {
            activeLeasesMac.remove(oldLease.macAddress);
            Log.log(Log.LOG_INFO, "Cleaned up stale MAC mapping for " + oldLease.macAddress);
        }

        Lease newLease = new Lease(ip, macAddress, Lease.LeaseState.OFFERED,
                                   timeSeconds, transID);

        // create new lease for this ip.
        activeLeasesIP.put(ip, newLease);
        activeLeasesMac.put(macAddress, newLease);

        Log.log(Log.LOG_INFO, "IP " + ip + " set to offered for MAC " + macAddress);

        return newLease;
    }

    // ONLY validates it's not < 6 bytes
    private String macToString(byte[] macAddress) {
        if (macAddress.length != 6) return null;
        else return java.util.HexFormat.ofDelimiter(":").formatHex(macAddress);
    }
}
