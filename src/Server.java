import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.*;
import java.time.format.DateTimeFormatter;

import DHCP.*;
import Common.*;

// This class is basically the main server stuff, easy (i'm going crazy)

public class Server {
    private Args opt;
    private DatagramSocket socket;

    public Server(Args opt, DatagramSocket socket) {

        if (opt == null || socket == null)  {
            Log.log(Log.LOG_ERROR, "Error on server creation: socket or args were null");
            throw new IllegalArgumentException();
        }

        this.opt = opt;
        this.socket = socket;
    }

    public void run() {

        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        DatagramPacket response;

        DHCP dhcp = null;
        if (!opt.sniffOnly) dhcp = new DHCP(opt);

        // to format times for printing
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        Log.log(Log.LOG_ALL, "Waiting for DHCP requests...");


        while (true) {

            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Log.log(Log.LOG_INFO,
                "Received request from " +
                packet.getAddress() + ":" + packet.getPort() +
                " at time " +
                ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).format(fmt)
            );

            if (dhcp == null) continue; // exit if only reading requests

            // if verbose, show info for received packets
            if (opt.verbose) System.out.println(new DecodedDHCP(packet.getData()).toString());

            response = dhcp.respondToMessage(packet);

            if (response == null) {
                Log.log(Log.LOG_INFO, "Packet generated no response");
                continue;
            }

            try {
                socket.send(response);
            } catch (IOException e) {
                Log.log(Log.LOG_ERROR, "Error while sending response: " + e.getMessage());
            }

        }
    }

}
