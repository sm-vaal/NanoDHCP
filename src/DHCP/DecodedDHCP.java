package DHCP;

import Common.*;
import java.net.DatagramPacket;

public class DecodedDHCP {

    public byte[] rawPacket;
    public final int optionsOffset = 240;
    public int totalLength;

    public static final int NO_TYPE      = 0;
    public static final int DHCPDISCOVER = 1;
    public static final int DHCPOFFER    = 2;
    public static final int DHCPREQUEST  = 3;
    public static final int DHCPDECLINE  = 4;
    public static final int DHCPACK      = 5;
    public static final int DHCPNAK      = 6;
    public static final int DHCPRELEASE  = 7;
    public static final int DHCPINFORM   = 8;

    public int messageType = 0;


    public int code;
    public int hwType;
    public int macLen;
    public int hops;

    public int transactionID;
    public int secsSinceClientBoot;
    public int flags;

    public int[] clientIP   = new int[4];
    public int[] assignedIP = new int[4];
    public int[] serverIP   = new int[4];
    public int[] routerIP   = new int[4];

    public byte[] clientMAC;
    public byte[] serverDomainNameBytes = new byte[64];
    public String serverDomainName;
    public byte[] bootFileNameBytes = new byte[128];
    public String bootFileName;

    public int[]   requestedIP = {0, 0, 0, 0};
    public boolean wasIPRequested = false;

    public int[]   parameterRequestList;
    public boolean wereParametersRequested = false;

    public boolean isValid = false;

    public byte[]  clientID;

    public DecodedDHCP(byte[] msg) {
        rawPacket   = msg; // rawPacket depends on msg lifetime, important
        totalLength = msg.length;

        if (msg.length < 236) {
            Log.log(Log.LOG_ERROR, "Message invalid on decode, size<236");
        }

        code   = Byte.toUnsignedInt(msg[0]);
        hwType = Byte.toUnsignedInt(msg[1]);
        macLen = Byte.toUnsignedInt(msg[2]);
        hops   = Byte.toUnsignedInt(msg[3]);

        transactionID =
            (msg[4] & 0xFF) << 24 |
            (msg[5] & 0xFF) << 16 |
            (msg[6] & 0xFF) << 8  |
            (msg[7] & 0xFF);

        secsSinceClientBoot =
            (msg[8] & 0xFF) << 8  |
            (msg[9] & 0xFF);

        flags =  // only 1st bit used. print as hex
            (msg[10] & 0xFF) << 8  |
            (msg[11] & 0xFF);

        for (int i = 0; i < 4; i++) {
            clientIP[i]   = Byte.toUnsignedInt(msg[12+i]);
            assignedIP[i] = Byte.toUnsignedInt(msg[16+i]);
            serverIP[i]   = Byte.toUnsignedInt(msg[20+i]);
            routerIP[i]   = Byte.toUnsignedInt(msg[24+i]);
        }

        macLen = Math.min(macLen, 16); // clamp mac length
        if (28 + macLen > msg.length) {
            Log.log(Log.LOG_ERROR, "Received mac length > 16    ");
            return;
        }

        clientMAC = new byte[macLen]; // max 16 bytes
        for (int i = 0; i < macLen; i++) {
            clientMAC[i] = msg[28+i];
        }

        for (int i = 0; i < 64; i++) {
            serverDomainNameBytes[i] = msg[44+i];
        }
        // convert to java String
        serverDomainName = new String(serverDomainNameBytes, java.nio.charset.StandardCharsets.US_ASCII);

        for (int i = 0; i < 128; i++) {
            bootFileNameBytes[i] = msg[108+i];
        }
        bootFileName = new String(bootFileNameBytes, java.nio.charset.StandardCharsets.US_ASCII);

        byte[] magicCookie = {99, -126, 83, 99};
        for (int i = 0; i < 4; i++) {
            if (magicCookie[i] != msg[236 + i]) {
                Log.log(Log.LOG_WARNING, "Magic cookie was incorrect - continuing anyway");
                break;
            }
        }


        // parse parameter request list
        for (int i = 240; i < rawPacket.length;) {

            int tag = Byte.toUnsignedInt(rawPacket[i]);

            if (tag == 0) {
                i++; // padding
                continue;
            } else if (tag == 255) {
                break; // end
            }

            FieldAttributes attr = DHCPOptionAttributes.attributes.get(tag);

            if (i + 1 >= rawPacket.length) {
                Log.log(Log.LOG_ERROR, "No length provided for last attribute");
                break;
            }

            int length = Byte.toUnsignedInt(rawPacket[i+1]);

            if (i + 2 + length > rawPacket.length) {
                Log.log(Log.LOG_ERROR, "Option had length greater than packet length");
                break;
            }

            if (tag == 53 && length == 1) {         // DHCP Message Type
                messageType = Byte.toUnsignedInt(rawPacket[i + 2]);
            }

            if (tag == 55) {                        // Requested params
                wereParametersRequested = true;
                parameterRequestList    = new int[length];
                for (int j = 0; j < length; j++) {
                    parameterRequestList[j] = Byte.toUnsignedInt(rawPacket[i + 2 + j]);
                }
            }

            if (tag == 50) {                        // Requested IP
                wasIPRequested = true;
                for (int j = 0; j < length; j++) {
                    requestedIP[j] = Byte.toUnsignedInt(rawPacket[i + 2 + j]);
                }
            }

            if (tag == 61) {                        // needed for PXE
                clientID = new byte[length];
                for (int j = 0; j < length; j++) {
                    clientID[j] = rawPacket[i + 2 + j];
                }
            }

            i += (2 + length);
        }


        isValid = true;
    }

    public DecodedDHCP(DatagramPacket packet) {
        this(packet.getData());
    }

    @Override
    public String toString() {

        if (!isValid) {
            Log.log(Log.LOG_ERROR, "Packet parsing error: could not stringify packet");
            return "";
        }

        if (rawPacket.length < 236) {
            return ("Message invalid, size<236");
        }

        StringBuilder sb = new StringBuilder();

        sb.append(
            "=== MISC ===\n" +
                "\tcode:           "  + code + "\n" +
                "\thwType:         "  + hwType + "\n" +
                "\tmacLen:         "  + macLen + "\n" +
                "\ttransID:        "  + ByteFuncts.toHexString(transactionID) + "\n" +
                "\tsec since boot: "  + secsSinceClientBoot + "\n" +
                "\tflags:          "  + ByteFuncts.toHexString(flags) + "\n" +
                "\tclient MAC:     "  + java.util.HexFormat.ofDelimiter(":").formatHex(clientMAC) + "\n" +
                "\tserver domain:  "  + serverDomainName + "\n" +
                "\tboot file:      "  + bootFileName + "\n" +

            "\n=== IPs ===\n" +
                "\tclient IP:      "  + IP.intArrToIP(clientIP) + "\n" +
                "\tassigned IP:    "  + IP.intArrToIP(assignedIP) + "\n" +
                "\tserver IP:      "  + IP.intArrToIP(serverIP) + "\n" +
                "\trouter IP:      "  + IP.intArrToIP(routerIP) + "\n"
        );


        // rest of non-standard options. Printed as hex always, i ain't making specifics thanks
        if (rawPacket.length > 236) sb.append("\n=== OPTIONS (in raw bytes) ===\n");
        else return sb.toString();

        // 99.130.83.99, but java has no unsigned

        for (int i = 240; i < rawPacket.length;) { // increment is manual in the loop

            if (rawPacket[i] == 0) {
                i++; // padding
                continue;
            } else if (rawPacket[i] == 255) {
                break; // end
            }

            int tag = Byte.toUnsignedInt(rawPacket[i]);
            FieldAttributes attr = DHCPOptionAttributes.attributes.get(tag);

            if (i + 1 >= rawPacket.length) {
                Log.log(Log.LOG_ERROR, "No length provided for last attribute");
                break;
            }

            int length = Byte.toUnsignedInt(rawPacket[i+1]);

            if (i + 2 + length > rawPacket.length) {
                Log.log(Log.LOG_ERROR, "Option had length greater than packet length");
                return sb.toString();
            }

            String optName;
            if (attr == null) {
                optName = "Unknown (" + length + ")";
            } else {
                optName = attr.name;
            }

            byte[] optBytes = new byte[length];

            for (int j = 0; j < length; j++) {
                optBytes[j] = rawPacket[i + 2 + j];
            }

            // Essentials are explicit, not bytes
            if (tag == 53 && length == 1) {         // DHCP message type
                int type = optBytes[0] & 0xFF;
                sb.append("\tDHCP msg type: ").append(switch(type) {
                    case 1 -> "DISCOVER";
                    case 2 -> "OFFER";
                    case 3 -> "REQUEST";
                    case 5 -> "ACK";
                    case 6 -> "NAK";
                    default -> "UNKNOWN";
                }).append("\n");
            } else if (tag == 50 && length == 4) {  // Requested IP
                int[] reqIP = new int[4];
                for (int j = 0; j < 4; j++) {
                    reqIP[j] = Byte.toUnsignedInt(rawPacket[i+2+j]);
                }
                sb.append("\tRequested IP: ").append(IP.intArrToIP(reqIP)).append("\n");
            } else { // the rest is for wireshark
                sb.append("\t").append(optName).append(":   ").append(java.util.HexFormat.ofDelimiter(" ").formatHex(optBytes)).append("\n");
            }

            i += (2 + length);
        }

        return sb.toString();

    }

    public String macToString() {
        return java.util.HexFormat.ofDelimiter(":").formatHex(clientMAC);
    }

}
