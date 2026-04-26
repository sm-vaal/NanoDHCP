package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;

public class ServerIdHandler_54 implements OptionHandler {
    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {

        byte[] ret = new byte[6];
        ret[0] = 54; // Tag 54: Server Identifier
        ret[1] = 4;  // Length: 4 bytes for an IPv4 address

        // Using placeholder 192.168.1.2 for now
        // TODO: add local IP here
        ret[2] = (byte) 192;
        ret[3] = (byte) 168;
        ret[4] = (byte) 1;
        ret[5] = (byte) 2;

        return ret;
    }
}
