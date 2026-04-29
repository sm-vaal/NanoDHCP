package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;

public class ClientID_61 implements OptionHandler {

    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {

        if (ctx.clientID == null || ctx.clientID.length == 0) {
            return null;
        }

        byte[] option = new byte[2 + ctx.clientID.length];
        
        option[0] = 61;
        option[1] = (byte) ctx.clientID.length;
        
        System.arraycopy(ctx.clientID, 0, option, 2, ctx.clientID.length); 
        
        return option;
    }
}