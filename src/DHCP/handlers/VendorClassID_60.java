package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;
import java.nio.charset.StandardCharsets;

public class VendorClassID_60 implements OptionHandler {

    private String pxeEnabled = "PXEClient\0";

    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {

        if (servOpt.servPxe != null) {
            byte[] stringBytes = (pxeEnabled).getBytes(StandardCharsets.US_ASCII);
            byte[] option = new byte[2 + stringBytes.length];
        
            option[0] = 60;
            option[1] = (byte) stringBytes.length;
            System.arraycopy(stringBytes, 0, option, 2, stringBytes.length); // Data
            return option;
        } else return null;
    }
}
