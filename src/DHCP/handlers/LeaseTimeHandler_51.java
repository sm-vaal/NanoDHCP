package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;

public class LeaseTimeHandler_51 implements OptionHandler {
    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {
        byte[] ret = new byte[6];
        ret[0] = 51; // code
        ret[1] = 4; // length

        ret[2] = (byte) (servOpt.leaseTime >>> 24);
        ret[3] = (byte) (servOpt.leaseTime >>> 16);
        ret[4] = (byte) (servOpt.leaseTime >>> 8);
        ret[5] = (byte)  servOpt.leaseTime;

        return ret;
    }
}
