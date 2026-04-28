import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import Common.*;

public class NanoDHCP {

    public static void main(String[] args) {

        Log.setLogLevel(Log.LOG_WARNING);

        // parse the stuff
        Args opt = new Args(args);

        // check basics
        if (opt.errorsFound) {
            opt.showUsage();
            System.exit(1);
        }

        if (opt.verbose) {
            Log.setLogLevel(Log.LOG_ALL);
            opt.showSelectedOptions();
        }

        Log.log(Log.LOG_INFO, "Starting DHCP server...");

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(opt.port);
        } catch (SocketException e) {
            Log.log(Log.LOG_ERROR, "Error binding to port " + opt.port + ": " + e.getMessage());
            if (opt.port < 1024) {
                Log.log(Log.LOG_ERROR, "Ports < 1024 require root privileges. Are you running as root/admin?");
                return;
            } else {
                throw new RuntimeException(e);
            }
        }

        Log.log(Log.LOG_ALL, "Successfully bound to port " + opt.port);

        try {
            socket.setBroadcast(true);
        } catch (SocketException e) {
            Log.log(Log.LOG_ERROR, "Error setting broadcast: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // To reuse a port in different IPs, when running in a system with
        // multiple interfaces
        try {
            socket.setReuseAddress(true);
        } catch (SocketException e) {
            Log.log(Log.LOG_ERROR, "Error setting reuse address: " + e.getMessage());
            throw new RuntimeException(e);
        }

        Server server = new Server(opt, socket);
        server.run();

    }


}
