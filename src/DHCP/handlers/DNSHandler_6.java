package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;

public class DNSHandler_6 implements OptionHandler {
    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {
        byte[] ret = new byte[6];
        ret[0] = 6; // code
        ret[1] = 4; // length
        for (int i = 0; i < 4; i++) {
            ret[i+2] = (byte) (servOpt.dns[i] & 0xFF);
        }

        return ret;
    }
}
