package DHCP;

public class DHCPContext {
    public int messageType;
    public int requestedIP;
    public byte[] requestList;

    public int clientIP;
    public int serverID;

    public DHCPContext(int messageType, int requestedIP, byte[] requestList, int clientIP, int serverID) {
        this.messageType = messageType;
        this.requestedIP = requestedIP;
        this.requestList = requestList;
        this.clientIP    = clientIP;
        this.serverID    = serverID;
    }

}
