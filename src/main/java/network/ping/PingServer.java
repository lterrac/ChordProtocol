package network.ping;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Server to process ping requests over UDP.
 */
public class PingServer implements Runnable {
    private static final Logger logger = Logger.getLogger(PingServer.class.getName());
    private DatagramSocket socket;
    private AtomicBoolean terminated;

    // Create a datagram socket for receiving and sending UDP packets
    public PingServer() {
        terminated = new AtomicBoolean(false);
        boolean createdDatagramSocket = false;
        while (!createdDatagramSocket) {
            try {
                socket = new DatagramSocket();

                System.out.println("Datagram server is up on:");
                System.out.println("Ip address: " + InetAddress.getLocalHost().getHostAddress());
                System.out.println("Port: " + socket.getLocalPort());

                createdDatagramSocket = true;
            } catch (SocketException e) {
                logger.log(Level.WARNING, "Failed to open the datagram socket server. I'll retry in a second!");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (UnknownHostException e) {
                logger.log(Level.SEVERE, "Unknown host exception");
            }
        }
    }

    public void startServer() {
        // Processing loop.
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

    public int getPort() {
        return socket.getLocalPort();
    }

    public void terminate() {
        terminated.set(true);
    }
}