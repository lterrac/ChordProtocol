package network.ping;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server to process ping requests over UDP
 */
public class PingServer implements Runnable {
    private static final Logger logger = Logger.getLogger(PingServer.class.getName());
    private DatagramSocket socket;
    private AtomicBoolean terminated;

    /**
     * Create a datagram socket for receiving and sending UDP packets to prove that the node on which is running is alive
     */
    public PingServer() {
        terminated = new AtomicBoolean(false);
        boolean createdDatagramSocket = false;
        while (!createdDatagramSocket) {
            try {
                socket = new DatagramSocket();
                printCoordinates();
                createdDatagramSocket = true;
            } catch (SocketException e) {
                logger.log(Level.WARNING, "Failed to open the datagram socket server. I'll retry in a second!");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * Print the ip address and port of the ping server
     */
    private void printCoordinates() {
        System.out.println("Datagram server is up on:");
        try {
            System.out.println("Ip address: " + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "Unknown host exception");
        }
        System.out.println("Port: " + socket.getLocalPort());
    }

    /**
     * Listen for ping requests and reply as soon as you receive them
     */
    private void startServer() {
        // Processing loop
        while (!terminated.get()) {
            // Create a datagram packet to hold incoming UDP packet.
            DatagramPacket request = new DatagramPacket(new byte[1024], 1024);

            // Block until the host receives a UDP packet.
            try {
                socket.receive(request);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error while receiving a packet");
            }

            // Send reply
            InetAddress clientHost = request.getAddress();
            int clientPort = request.getPort();
            byte[] buf = request.getData();
            DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
            try {
                socket.send(reply);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error while sending a reply");
            }
        }
        socket.close();
    }

    @Override
    public void run() {
        startServer();
    }

    /**
     * @return the port on which the server is listening
     */
    public int getPort() {
        return socket.getLocalPort();
    }

    /**
     * Terminate the listening loop of the server
     */
    public void terminate() {
        terminated.set(true);
    }
}