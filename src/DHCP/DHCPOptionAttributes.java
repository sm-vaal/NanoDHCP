package DHCP;

import java.util.Map;
import java.util.HashMap;
import java.net.DatagramPacket;

import Common.*;
import DHCP.handlers.*;

public final class DHCPOptionAttributes {
    public static Map<Integer, FieldAttributes> attributes = new HashMap<Integer, FieldAttributes>();
    public static Map<Integer, OptionHandler>   handlers   = new HashMap<Integer, OptionHandler>();

    // Most options are not implemented, just the very basic ones for network config
    // this is useful mostly for displaying packets

    static {
        // === ATTRIBUTES ===
        attributes.put(0  , new FieldAttributes("Padding",         1, false ));
        attributes.put(1  , new FieldAttributes("Subnet mask",     4, false ));
        attributes.put(2  , new FieldAttributes("Time offset",     4, false ));
        attributes.put(3  , new FieldAttributes("Router",          4, true  ));
        attributes.put(4  , new FieldAttributes("Time server",     4, true  ));
        attributes.put(5  , new FieldAttributes("Name server",     4, true  ));
        attributes.put(6  , new FieldAttributes("DNS server",      4, true  ));
        attributes.put(7  , new FieldAttributes("Log server",      4, true  ));
        attributes.put(8  , new FieldAttributes("Quotes server",   4, true  ));
        attributes.put(9  , new FieldAttributes("LPR server",      4, true  ));
        attributes.put(10 , new FieldAttributes("Impress server",  4, true  ));
        attributes.put(11 , new FieldAttributes("RLP server",      4, true  ));
        attributes.put(12 , new FieldAttributes("Hostname string", 1, true  ));
        attributes.put(13 , new FieldAttributes("Boot file size",  2, false ));
        attributes.put(14 , new FieldAttributes("Merit dump file", 1, true  ));
        attributes.put(15 , new FieldAttributes("Domain name",     1, true  ));

        // ...

        attributes.put(50 , new FieldAttributes("Requested IP"  ,  4, false ));
        attributes.put(51 , new FieldAttributes("Lease time"    ,  4, false ));
        // 52 not needed

        attributes.put(53 , new FieldAttributes("DHCP msg type" ,  1, false ));
        attributes.put(54 , new FieldAttributes("Server ID"     ,  4, false ));
        attributes.put(55 , new FieldAttributes("Param. req. lst", 1, true));

        // ...

        attributes.put(255, new FieldAttributes("End of packet",   1, false ));

        // === HANDLERS ===
        handlers.put(1,  new SubnetMaskHandler_1());
        handlers.put(3,  new GatewayHandler_3());
        handlers.put(6,  new DNSHandler_6());
        handlers.put(51, new LeaseTimeHandler_51());
        handlers.put(54, new ServerIdHandler_54());


        Log.log(Log.LOG_INFO, "Field attribute and handler tables successfully filled");
    }

    private DHCPOptionAttributes() {}
}
