package DHCP;

import java.util.ArrayList;
import java.nio.ByteBuffer;


import Common.*;
import DHCP.*;
import DHCP.lease.*;
import java.nio.charset.StandardCharsets;

public class DHCPPacketBuilder {

    DecodedDHCP dec;
    Args opt;
    Lease l;

    private final byte code      = 2; // all server responses are 2
    private final byte hwType    = 1; // ethernet, always
    private final byte macLength = 6;
    private final byte hops      = 0;

    public int transactionID;
    public int secsSinceClientBoot;
    public int flags;

    private final int[] clientIP  = {0, 0, 0, 0};
    public int[] assignedIP       = {0, 0, 0, 0};
    public int[] serverIP         = {0, 0, 0, 0};
    private static int[] routerIP = {0, 0, 0, 0};

    public byte[] clientMAC; // copy

    private final byte[] magicCookie = {99, -126, 83, 99};

    private int outgoingMessageType;

    // empty for now
    public byte[] serverDomainNameBytes = new byte[64];
    public String serverDomainName;
    public byte[] bootFileNameBytes = new byte[128];
    public String bootFileName;

    ArrayList<byte[]> optBytes = new ArrayList<byte[]>();

    // constructor takes "decoded" options and sets them into the builder
    public DHCPPacketBuilder(DecodedDHCP dec, Args opt, Lease lease, int outgoingMessageType) {
        if (dec == null || opt == null || lease == null) throw new IllegalArgumentException();

        this.opt = opt;

        this.dec                 = dec;
        this.transactionID       = dec.transactionID;
        this.secsSinceClientBoot = dec.secsSinceClientBoot;
        this.flags               = dec.flags;
        this.clientMAC           = dec.clientMAC;

        this.bootFileName        = opt.filePxe;

        this.outgoingMessageType = outgoingMessageType;

        this.assignedIP = IP.parseIP(lease.ipAddress);

        this.serverIP = IP.parseIP(opt.serverIP);


        // fill bytes if pxe enabled
        if (opt.servPxe != null) {
            byte[] fileBytes = opt.filePxe.getBytes(StandardCharsets.US_ASCII);
            int lengthToCopy = Math.min(fileBytes.length, 127);
            System.arraycopy(opt.filePxe.getBytes(StandardCharsets.US_ASCII), 0, bootFileNameBytes, 0, lengthToCopy); 

            System.arraycopy(opt.servPxeIP, 0, serverIP, 0, 4); 
        }
    }

    public byte[] build() {
        this.generateOptBytes();

        // total length of dynamic options
        int optionsLength = 0;
        for (byte[] opt : optBytes) {
            optionsLength += opt.length;
        }

        // Allocate buffer: fixed + cookie + msgType + variable + end
        ByteBuffer buf = ByteBuffer.allocate(240 + 3 + optionsLength + 1);

        // 236 Bytes fixed
        buf.put(code);
        buf.put(hwType);
        buf.put(macLength);
        buf.put(hops);

        buf.putInt(transactionID);           // ByteBuffer automatically splits into 4 bytes
        buf.putShort((short) secsSinceClientBoot);
        buf.putShort((short) flags);

        // IPs
        for (int b : clientIP)   buf.put((byte) b);
        for (int b : assignedIP) buf.put((byte) b);
        for (int b : serverIP)   buf.put((byte) b);
        for (int b : routerIP)   buf.put((byte) b);

        // Mac padded to 16 bytes
        byte[] chaddr = new byte[16];
        System.arraycopy(clientMAC, 0, chaddr, 0, clientMAC.length);
        buf.put(chaddr);

        // Server Name (64) & Boot File (128) - Just write empty zeros
        buf.put(serverDomainNameBytes);
        buf.put(bootFileNameBytes);

        // magic cookie, 4 Bytes
        buf.put(new byte[]{ 99, (byte) 130, 83, 99 });

        // msg type
        buf.put(new byte[] { 53, 1, (byte) outgoingMessageType});

        // dynamic opt
        for (byte[] o : optBytes) {
            buf.put(o);
        }

        buf.put((byte) 255);

        return buf.array();
    }

    public void generateOptBytes() {
        OptionHandler serverId = DHCPOptionAttributes.handlers.get(54);

        if (serverId != null) optBytes.add(serverId.handle(dec, l, opt));

        OptionHandler leaseTime = DHCPOptionAttributes.handlers.get(51);

        if (leaseTime != null) optBytes.add(leaseTime.handle(dec, l, opt));

        if (!dec.wereParametersRequested || dec.parameterRequestList == null) return;

        byte[] in;
        for (int param : dec.parameterRequestList) {
            if (param == 53 || param == 54 || param == 51) continue;

            OptionHandler h = DHCPOptionAttributes.handlers.get(param);
            if (h != null) {
                in = h.handle(dec, l, opt);
                if (in != null) optBytes.add(in);
            }
        }

    }

}
