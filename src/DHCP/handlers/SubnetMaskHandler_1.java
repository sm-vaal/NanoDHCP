package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;

public class SubnetMaskHandler_1 implements OptionHandler {
    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {
        byte[] ret = new byte[6];
        ret[0] = 1;
        ret[1] = 4;

        int mask = servOpt.netmask == 0 ? 0 : (~0 << (32 - servOpt.netmask));

        ret[2] = (byte) (mask >>> 24);
        ret[3] = (byte) (mask >>> 16);
        ret[4] = (byte) (mask >>> 8);
        ret[5] = (byte)  mask;

        return ret;
    }
}
