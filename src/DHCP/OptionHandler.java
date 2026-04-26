package DHCP;

import Common.Args;
import DHCP.lease.Lease;

public interface OptionHandler {
    byte[] handle(DecodedDHCP ctx, Lease l, Args servOpt);
}
