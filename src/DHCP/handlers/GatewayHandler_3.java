package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;

public class GatewayHandler_3 implements OptionHandler {
    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {
        byte[] ret = new byte[6];
        ret[0] = 3;
        ret[1] = 4;

        for (int i = 0; i < 4; i++) {
            ret[i+2] = (byte) servOpt.gateway[i];
        }

        return ret;
    }
}
