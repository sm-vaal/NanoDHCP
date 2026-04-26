package DHCP.lease;

public class Lease {

    public enum LeaseState {
        AVAILABLE,
        OFFERED,
        BOUND,
        EXPIRED
    }

    public String     ipAddress;
    public String     macAddress;
    public LeaseState leaseState = LeaseState.AVAILABLE;
    public long       expirationTimeEpoch;
    public long       transactionId;

    public Lease(String ip, String mac, LeaseState ls,
                 int timeSeconds, long transId) {

        ipAddress  = ip;
        macAddress = mac;
        leaseState = ls;
        expirationTimeEpoch = System.currentTimeMillis() + timeSeconds * 1000L;
        transactionId = transId;
    }

    public boolean isExpired() {
        if (System.currentTimeMillis() >= this.expirationTimeEpoch) leaseState = LeaseState.EXPIRED;
        return leaseState == LeaseState.EXPIRED;

    }

    public boolean isAvailable() {
        return (this.leaseState == LeaseState.AVAILABLE) ||
                (this.leaseState == LeaseState.EXPIRED);
    }

    // available FOR THIS MAC
    public boolean isAvailable(String mac) {
        if (this.leaseState == LeaseState.AVAILABLE ||
            this.leaseState == LeaseState.EXPIRED) {
            return true;
        }

        return mac != null && mac.equals(this.macAddress);
    }

}
