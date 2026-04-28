package DHCP.handlers;

import Common.*;
import DHCP.*;
import DHCP.lease.*;

public class BootFileNamePXE_67 implements OptionHandler {
    public byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt) {
        // null terminator for file string, just in case
        // some servers break without it
        
        byte[] stringBytes = (servOpt.filePxe + "\0").getBytes(StandardCharsets.US_ASCII);
        byte[] option = new byte[2 + stringBytes.length];
    
        option[0] = 67;
        option[1] = (byte) stringBytes.length;
        System.arraycopy(stringBytes, 0, option, 2, stringBytes.length); // Data
        return option;
    }
}
