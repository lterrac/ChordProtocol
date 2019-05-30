package network;

import model.Node;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PingClient implements Runnable {
    private static final int PING_PERIOD = 10000;
    private static final int PING_LIMIT = 7;
    private static final int TIME_LIMIT = 1000;
    private static final Logger logger = Logger.getLogger(PingClient.class.getName());
    private DatagramSocket socket;
    private String successorIp;
    private int successorPort;
    private Node node;
    private boolean alive;

    // Create a datagram socket for receiving and sending UDP packets
    public PingClient(Node node, String ip, int port) {
        this.successorIp = ip;
        this.successorPort = port;
        this.node = node;

        boolean stop = false;

        while (!stop) {
            try {
                socket = new DatagramSocket();
                System.out.println("Datagram client is up on:");
                System.out.println("Ip address: " + InetAddress.getLocalHost().getHostAddress());
                System.out.println("Port: " + socket.getLocalPort());
                System.out.println("Pinging towards: ");
                System.out.println("Ip address: " + successorIp);
                System.out.println("Port: " + successorPort);

                stop = true;
            } catch (SocketException e) {
                logger.log(Level.WARNING, "Failed to open the datagram socket client. I'll retry in a second!");
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

    public void startClient() {
        alive = true;
        while (alive) {
            alive = ping();
            // wait PING_PERIOD seconds
            if(alive) {
                try {
                    Thread.sleep(PING_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // the node has crashed
        node.replaceSuccessor();
    }

    public boolean ping() {
        int missed = 0;
        for (int i = 0; i < PING_LIMIT; i++) {
            long SendTime = System.currentTimeMillis();
            String Message = "Ping " + i + " " + SendTime + "\n";

            // create the request
            DatagramPacket request = null;
            try {
                request = new DatagramPacket(Message.getBytes(), Message.length(), InetAddress.getByName(successorIp), successorPort);
            } catch (UnknownHostException e) {
                logger.log(Level.SEVERE, "Unknown host exception");
                e.printStackTrace();
            }

            // send the request and wait for the reply
            try {
                socket.send(request);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error while sending a request");
                e.printStackTrace();
            }
            DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);

            try {
                socket.setSoTimeout(TIME_LIMIT);
            } catch (SocketException e) {
                logger.log(Level.SEVERE, "The packet " + i + " didn't receive a reply in time.");
            }

            try {
                socket.receive(reply);
                //printData(reply);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Missing packet");
                missed++;
            }

            // Wait one second to send the next message
            try {
                Thread.sleep(TIME_LIMIT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // simple check to detect a the crash of a node
        return missed != PING_LIMIT;
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
        startClient();
    }

    public void stop() {
        alive = false;
    }
}