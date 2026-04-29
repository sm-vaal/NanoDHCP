package DHCP;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import Common.*;
import DHCP.lease.*;

public class DHCP {

    protected Args opt; // the settings are stored on the parsed args object

    // some "raw" server settings as ints
    protected int startIP = 0;
    protected int endIP   = 0;
    protected int mask    = 0;
    protected int gateway = 0;
    protected int dns     = 0;

    LeaseManager lm;

    InetAddress broadcast;

    public DHCP(Args opt) throws IllegalArgumentException {
        this.opt = opt;

        try {
            startIP = IP.ipToInt(opt.ipStart);
            endIP   = IP.ipToInt(opt.ipEnd);
            gateway = IP.ipToInt(opt.gateway);
            dns     = IP.ipToInt(opt.dns);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid IPs for DHCP server creation");
        }

        mask    = opt.netmask == 0 ? 0 : (~0 << (32 - opt.netmask));

        lm = new LeaseManager(opt.ipStart, opt.ipEnd, opt.netmask);

        int broadcastInt = (startIP & mask) | ~mask;

        String broadcastStr = ((broadcastInt >>> 24) & 0xFF) + "." +
                              ((broadcastInt >>> 16) & 0xFF) + "." +
                              ((broadcastInt >>> 8)  & 0xFF) + "." +
                              (broadcastInt & 0xFF);

        try {
            broadcast = InetAddress.getByName(broadcastStr); 
            Log.log(Log.LOG_INFO, "Calculated broadcast address: " + broadcastStr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.log(Log.LOG_ERROR, "couldn't find broadcast. WTF?");
        }

        Log.log(Log.LOG_INFO, "DHCP server created");
    }

    // returns the message to be sent. returns null if no response needed
    public DatagramPacket respondToMessage(DatagramPacket msg) {
        DecodedDHCP rcvPkt = new DecodedDHCP(msg);

        switch (rcvPkt.messageType) {
            case DecodedDHCP.DHCPDISCOVER: {
                // server holds offer for 16 seconds
                Lease leaseToAssign = lm.assignToFirstLease(rcvPkt.macToString(), 16, rcvPkt.transactionID);
                if (leaseToAssign == null) {
                    Log.log(Log.LOG_WARNING, "No lease could be offered to " + rcvPkt.macToString());
                    return null;
                }

                DHCPPacketBuilder b = new DHCPPacketBuilder(rcvPkt, opt, leaseToAssign, DecodedDHCP.DHCPOFFER);

                byte[] bBuilt = b.build();

                return new DatagramPacket (bBuilt, bBuilt.length, broadcast, 68);
            }

            case DecodedDHCP.DHCPREQUEST: {
                int[] reqIP;

                // connecting for the first time (opt 50)
                if (rcvPkt.wasIPRequested) {
                    reqIP = rcvPkt.requestedIP;
                }
                // renewing an existing lease (no opt 50)
                else if (IP.ipToInt(rcvPkt.clientIP) != 0) {
                    reqIP = rcvPkt.clientIP;
                }
                else {
                    Log.log(Log.LOG_WARNING, "DHCPREQUEST by " + rcvPkt.macToString() + " but no IP was provided anywhere");
                    return null;
                }

                int cod = DecodedDHCP.DHCPACK;

                // technically requestedLease for now
                Lease assignedLease = lm.getLeaseByIP(reqIP);

                if (IP.ipToInt(reqIP) < startIP ||
                    IP.ipToInt(reqIP) > endIP) {
                        Log.log(Log.LOG_INFO, "DHCPREQUEST by " + rcvPkt.macToString() +
                                " rejected: IP " + IP.intArrToIP(reqIP) + " not in range");
                        cod = DecodedDHCP.DHCPNAK;
                }

                // ensure we can assign: is this IP available?
                if (assignedLease == null) {  
                    // if never assigned, assign 
                    assignedLease = lm.assignToLease(IP.intArrToIP(reqIP), rcvPkt.macToString(), opt.leaseTime, rcvPkt.transactionID, Lease.LeaseState.BOUND);
                } else {
                    // if it has ever been assigned, only valid if available
                    if (assignedLease.isAvailable(rcvPkt.macToString())) {
                        assignedLease = lm.assignToLease(IP.intArrToIP(reqIP), rcvPkt.macToString(), opt.leaseTime, rcvPkt.transactionID, Lease.LeaseState.BOUND);
                    } else {
                        Log.log(Log.LOG_INFO, "DHCPREQUEST by " + rcvPkt.macToString() +
                                " rejected: IP " + IP.intArrToIP(reqIP) + " not available");
                        cod = DecodedDHCP.DHCPNAK;
                    }
                }

                DHCPPacketBuilder b = new DHCPPacketBuilder(rcvPkt, opt, assignedLease, cod);

                byte[] bBuilt = b.build();

                Log.log(Log.LOG_INFO, "MAC " + rcvPkt.macToString() + " -> IP " + IP.intArrToIP(reqIP));

                return new DatagramPacket (bBuilt, bBuilt.length, broadcast, 68);

            }

            // no action taken, not strictly needed with the round robin we use
            case DecodedDHCP.DHCPDECLINE: {
                String declinedIP = IP.intArrToIP(rcvPkt.requestedIP);

                Log.log(Log.LOG_INFO, "MAC " + rcvPkt.macToString() + " declined IP " + declinedIP);

                return null;
            }

            case DecodedDHCP.DHCPRELEASE: {
                String freedIP = IP.intArrToIP(rcvPkt.clientIP);

                Lease ls = lm.getLeaseByIP(freedIP);

                if (ls != null) {
                    ls.leaseState = Lease.LeaseState.AVAILABLE;
                }

                Log.log(Log.LOG_INFO, "MAC " + rcvPkt.macToString() + " freed IP " + freedIP);

                return null; // no response to a release
            }

            // not implemented, this server is minimal
            case DecodedDHCP.DHCPINFORM: {
                return null;
            }

            default:
        }

        return null;
    }
}
