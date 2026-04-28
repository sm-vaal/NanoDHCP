package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;

public class TFTPServerNamePXE_66 implements OptionHandler {
    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {
        byte[] stringBytes = servOpt.servPxe.getBytes(StandardCharsets.US_ASCII);
        byte[] option = new byte[2 + stringBytes.length];
    
        option[0] = 66;
        option[1] = (byte) stringBytes.length;
        System.arraycopy(stringBytes, 0, option, 2, stringBytes.length); // Data
        return option;
    }
}
