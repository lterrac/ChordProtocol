package network.ping;

import model.Node;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class PingClient implements Runnable {

    private static final int PING_PERIOD = 4000;
    private static final int PING_LIMIT = 5;
    private static final int TIME_LIMIT = 700;
    private static final Logger logger = Logger.getLogger(PingClient.class.getName());
    int targetNodeId;
    private DatagramSocket socket;
    String targetIp;
    int targetPort;
    Node node;
    private boolean alive;
    private AtomicBoolean terminated;

    // Create a datagram socket for receiving and sending UDP packets
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
        /*        System.out.println("Datagram client is up on:");
                System.out.println("Ip address: " + InetAddress.getLocalHost().getHostAddress());
                System.out.println("Port: " + socket.getLocalPort());
                System.out.println("Pinging towards: ");
                System.out.println("Ip address: " + targetIp);
                System.out.println("Port: " + targetPort);
*/
                createdDatagramSocket = true;
            } catch (SocketException e) {
                logger.log(Level.WARNING, "Failed to open the datagram socket client. I'll retry in a second!");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }/* catch (UnknownHostException e) {
                logger.log(Level.SEVERE, "Unknown host exception");
            }*/
        }
    }

    @Override
    public void run() {
        startClient();
    }

    public void terminate() {
        terminated.set(true);
    }


    public void startClient() {
        alive = true;
        while (alive && !terminated.get()) {
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
        if (!terminated.get())
            crashHandling();

        socket.close();
    }

    abstract void crashHandling();

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
                //       printAMiss(logger);
                missed++;
            }

            // Wait one second to send the next message
            try {
                Thread.sleep(TIME_LIMIT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        printMissingPackets(missed);

        // simple check to detect a the crash of a node
        return missed != PING_LIMIT;
    }

    protected abstract void printMissingPackets(int missed);

    protected abstract void printAMiss(Logger logger);
}


    /*
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
    } */
