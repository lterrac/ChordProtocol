package network.ping;

import model.Node;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class PingClient implements Runnable {

    static final Logger logger = Logger.getLogger(PingClient.class.getName());
    // waiting time after having sent a series of ping requests
    private static final int PING_PERIOD = 4000;
    // number of packets sent during a ping session
    private static final int PING_LIMIT = 5;
    // waiting time limit to receive the reply sent from the node you are pinging
    private static final int TIME_LIMIT = 700;
    int targetNodeId;
    String targetIp;
    int targetPort;
    Node node;
    private DatagramSocket socket;
    private boolean alive;
    private AtomicBoolean terminated;

    /**
     * Create a datagram socket for receiving and sending UDP packets
     */
    public PingClient(Node node, String ip, int port, int targetNodeId) {
        this.targetIp = ip;
        this.targetPort = port;
        this.node = node;
        this.targetNodeId = targetNodeId;

        terminated = new AtomicBoolean(false);
        boolean createdDatagramSocket = false;

        while (!createdDatagramSocket) {
            try {
                socket = new DatagramSocket();
                createdDatagramSocket = true;
            } catch (SocketException e) {
                logger.log(Level.WARNING, "Failed to open the datagram socket client. I'll retry in a second!");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        startClient();
    }

    /**
     * Terminate the listening loop of the client
     */
    public void terminate() {
        terminated.set(true);
    }

    /**
     * Start the pinging infrastructure.
     */
    public void startClient() {
        alive = true;
        while (alive && !terminated.get()) {
            alive = ping();
            // wait PING_PERIOD seconds
            if (alive) {
                try {
                    Thread.sleep(PING_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // the node has crashed
        if (!terminated.get())
            crashHandling();

        socket.close();
    }

    /**
     * After the discovery of a crashed node, execute the required action
     */
    abstract void crashHandling();

    /**
     * Every {@code PING_PERIOD} ping the {@code targetNodeId} node, sending {@code PING_LIMIT} requests
     *
     * @return false if the pinged node is alive
     */
    public boolean ping() {
        int missed = 0;
        for (int i = 0; i < PING_LIMIT; i++) {
            long sendTime = System.currentTimeMillis();
            String message = "Ping " + i + " " + sendTime + "\n";

            // create the request
            DatagramPacket request = null;
            try {
                request = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(targetIp), targetPort);
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

                socket.receive(reply);

            } catch (SocketException e) {
                logger.log(Level.SEVERE, "The packet " + i + " didn't receive a reply in time.");
                missed++;
            } catch (IOException e) {
                printAMiss(logger);
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

    /**
     * Print the packet that does not receive a reply from the server if the target node
     */
    protected abstract void printAMiss(Logger logger);
}
