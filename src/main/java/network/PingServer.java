package network;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Server to process ping requests over UDP.
 */
public class PingServer implements Runnable {
    private static final Logger logger = Logger.getLogger(PingServer.class.getName());
    private DatagramSocket socket;

    // Create a datagram socket for receiving and sending UDP packets
    public PingServer() {
        boolean stop = false;
        while (!stop) {
            try {
                socket = new DatagramSocket();

                System.out.println("Datagram server is up on:");
                System.out.println("Ip address: " + InetAddress.getLocalHost().getHostAddress());
                System.out.println("Port: " + socket.getLocalPort());

                stop = true;
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
        while (true) { // TODO: fallo terminare (atomicBoolean)
            // Create a datagram packet to hold incoming UDP packet.
            DatagramPacket request = new DatagramPacket(new byte[1024], 1024);

            // Block until the host receives a UDP packet.
            try {
                socket.receive(request);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error while receiving a packet");
            }

            // Print the received data.
            //printData(request);

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

          //  System.out.println("Reply sent.");
        }
    }

    // Print ping data to the standard output stream. // TODO: just for testing
    private void printData(DatagramPacket request) {
        // Obtain references to the packet's array of bytes.
        byte[] buf = request.getData();

        // Wrap the bytes in a byte array input stream, so that you can read the data as a stream of bytes.
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);

        // Wrap the byte array output stream in an input stream reader, so you can read the data as a stream of characters.
        InputStreamReader isr = new InputStreamReader(bais);

        // Wrap the input stream reader in a buffered reader, so you can read the character data a line at a time.
        BufferedReader br = new BufferedReader(isr);

        // The message data is contained in a single line, so read this line.
        String line = null;
        try {
            line = br.readLine();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while reading a packet");
        }

        // Print host address and data received from it.
        System.out.println(
                "Received from " +
                        request.getAddress().getHostAddress() +
                        ": " +
                        line);
    }

    @Override
    public void run() {
        startServer();
    }

    public int getPort() {
        return socket.getLocalPort();
    }
}