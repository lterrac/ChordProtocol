package model;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static model.NodeProperties.*;
import static utilities.Utilities.sha1;

public class Node {

    private static final Logger logger = Logger.getLogger(Node.class.getName());
    /**
     * Finger table of the node
     */
    private final NodeProperties[] fingers = new NodeProperties[KEY_SIZE];

    /**
     * Represents the "client side" of a node. It sends requests to other nodes
     */
    private Forwarder forwarder;
    /**
     * Contains information about the node
     */
    private NodeProperties properties;
    /**
     * Data contained in the node
     */
    private HashMap<Integer, Serializable> data;
    /**
     * List of adjacent successors of the node
     */
    private List<NodeProperties> successors;

    /**
     * Scheduled executor to run threads at regular time intervals
     */
    private ScheduledExecutorService checkPredecessorThread;
    private ScheduledExecutorService fixFingersThread;
    private ScheduledExecutorService stabilizeThread;

    /**
     * Classes containing the threads code
     */
    private CheckPredecessor checkPredecessor;
    private FixFingers fixFingers;
    private Stabilize stabilize;
    private NodeProperties predecessor;

    private NodeSocketServer nodeSocketServer;
    private ServerSocket serverSocket;

    /**
     * Useful to save the index of the finger table to which to apply the fix_finger algorithm
     */
    private int n_fix;

    public Node() {
        n_fix = -1;
        successors = new ArrayList<>();
        data = new HashMap<>();

        checkPredecessor = new CheckPredecessor(this);
        fixFingers = new FixFingers(this);
        stabilize = new Stabilize(this);
    }

    // Getter
    NodeProperties getProperties() {
        return properties;
    }

    ServerSocket getServerSocket() {
        return serverSocket;
    }

    NodeProperties getPredecessor() {
        return predecessor;
    }

    // Setter
    public void setPredecessor(NodeProperties predecessor) {
        this.predecessor = predecessor;
        //TODO:inviare al predecessore le risorse che spettano a lui
    }

    /**
     * Set the successor of the current node
     *
     * @param node the successor
     */
    void setSuccessor(NodeProperties node) {
        // TODO synchronized??
        this.fingers[0] = node;
    }

    NodeProperties successor() {
        return fingers[0];
    }

    /**
     * Get the Ip address of the current node
     *
     * @return the Ip address of the machine on which the node is running
     */
    private String getCurrentIp() {
        //Find Ip address, it will be published later for joining
        InetAddress currentIp = null;
        try {
            currentIp = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        }

        assert currentIp != null;
        return currentIp.getHostAddress();
    }

    /**
     * Update the predecessor node of the successor in the stabilize thread
     *
     * @param newNode is the node to be set as predecessor for the successor
     */
    void finalizeStabilize(NodeProperties newNode) {
        stabilize.finalizeStabilize(newNode);
    }

    /**
     * Create a new server socket that implements the server side of a node
     *
     * @return a new {@code ServerSocket}
     */
    private ServerSocket createServerSocket() {
        ServerSocket serverSocket = null;

        // Create the new serverSocket
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return serverSocket;
    }

    /**
     * Create a new Chord Ring
     */
    void create() {

        startNode();

        startThreads();

        nodeSocketServer = new NodeSocketServer(this);

        new Thread(nodeSocketServer).start();
    }

    /**
     * Join a Ring containing the known Node
     */
    void join(String ip, int port) {
        startNode();
        forward(properties, ip, port, "find_successor", 0, 0, 0, null);

        startThreads();
        nodeSocketServer = new NodeSocketServer(this);

        new Thread(nodeSocketServer).start();
        distributeResources(false);
    }

    private void startNode() {
        serverSocket = createServerSocket();
        forwarder = new Forwarder();
        int port = serverSocket.getLocalPort();
        String ipAddress = getCurrentIp();

        initializeNode(ipAddress, port);
        createResources();
    }

    private void initializeNode(String ipAddress, int port) {
        this.properties = new NodeProperties(sha1(ipAddress + ":" + port), ipAddress, port);
        this.setSuccessor(this.properties);
        this.predecessor = null;
    }

    private void createResources() {
        for (int i = 0; i < RESOURCESNUMBER; i++) {

            String filename = "File" + i;
            File f = new File("./node" + this.getProperties().getNodeId() + "files/" + filename);
            if (!f.getParentFile().exists())
                f.getParentFile().mkdirs();
            if (!f.exists()) {
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void distributeResources(boolean leaving) {
        if (leaving == true) {
            File folder = new File("./node" + this.getProperties().getNodeId() + "files");
            File[] allFiles = folder.listFiles();
            forward(null, successor().getIpAddress(), successor().getPort(), "transfer_files", 0, 0, 0, allFiles);
            for (int i = 0; i < RESOURCESNUMBER; i++) {
                File f = allFiles[i];
                f.delete();
            }
        }
    }

    public void notifyNeighbours() {
        forward(successor(), predecessor.getIpAddress(), predecessor.getPort(), "update_successor", 0, 0, 0, null);
        forward(getPredecessor(), successor().getIpAddress(), successor().getPort(), "update_predecessor", 0, 0, 0, null);
    }


    private void startThreads() {
        checkPredecessorThread = Executors.newSingleThreadScheduledExecutor();
        fixFingersThread = Executors.newSingleThreadScheduledExecutor();
        stabilizeThread = Executors.newSingleThreadScheduledExecutor();

        checkPredecessorThread.scheduleAtFixedRate(checkPredecessor, 0, CHECK_PERIOD, TimeUnit.MILLISECONDS);
        fixFingersThread.scheduleAtFixedRate(fixFingers, 2000, FIX_PERIOD, TimeUnit.MILLISECONDS);
        stabilizeThread.scheduleAtFixedRate(stabilize, 4000, STABILIZE_PERIOD, TimeUnit.MILLISECONDS);
    }


    /**
     * Notify the current node that a new predecessor can exists fot itself
     *
     * @param predecessor node that could be the predecessor
     */
    void notifySuccessor(NodeProperties predecessor) {
        if ((this.predecessor == null || predecessor.isInIntervalStrict(this.predecessor.getNodeId(), properties.getNodeId()))) {
            setPredecessor(predecessor);
        }
    }

    /**
     * Find the successor of the node with the given id
     *
     * @param askingNode is the node that asked for findings its successor
     */
    void findSuccessor(NodeProperties askingNode) {

        // check if there are two nodes with the same Id
        if (askingNode.getNodeId() == properties.getNodeId())
            logger.log(Level.SEVERE, "inconsistency: two nodes with the same ID");

        if (askingNode.isInInterval(properties.getNodeId(), successor().getNodeId())) {
            forward(successor(), askingNode.getIpAddress(), askingNode.getPort(), "find_successor_reply", 0, 0, 0, null);
        } else {
            NodeProperties closest = closestPrecedingNode(askingNode.getNodeId());

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties))
                forward(askingNode, closest.getIpAddress(), closest.getPort(), "find_successor", 0, 0, 0, null);
            else
                forward(properties, askingNode.getIpAddress(), askingNode.getPort(), "find_successor_reply", 0, 0, 0, null);
        }
    }

    /**
     * Find the successor of the the askingNode to update its finger table
     *
     * @param askingNode is the node that has sent the first fix_finger request
     * @param fixId      is the upper bound Id of the fixIndex-th row of the finger table
     * @param fixIndex   is the index of the finger table to be updated
     */
    void fixFingerSuccessor(NodeProperties askingNode, int fixId, int fixIndex) {

        if (NodeProperties.isInIntervalInteger(properties.getNodeId(), fixId, successor().getNodeId())) {
            forward(successor(), askingNode.getIpAddress(), askingNode.getPort(), "fix_finger_reply", fixId, fixIndex, 0, null);
        } else {
            NodeProperties closest = closestPrecedingNode(fixId);

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties)) {
                forward(askingNode, closest.getIpAddress(), closest.getPort(), "fix_finger", fixId, fixIndex, 0, null);
            } else {
                forward(properties, askingNode.getIpAddress(), askingNode.getPort(), "fix_finger_reply", fixId, fixIndex, 0, null);
            }
        }
    }

    /**
     * Find the highest predecessor of id.
     *
     * @param nodeId is the id of the target node
     * @return the closest node to the target one
     */
    private NodeProperties closestPrecedingNode(int nodeId) {
        for (int i = KEY_SIZE - 1; i >= 0; i--) {
            if (fingers[i] != null && fingers[i].isInInterval(properties.getNodeId(), nodeId))
                return fingers[i];
        }
        return properties;
    }


    /**
     * Look for the owner of a resource in the net.
     *
     * @param key is the hash of the resource you're looking for in the net
     */
    void lookup(NodeProperties askingNode, int key) {

        if (key > properties.getNodeId() && successor().getNodeId() >= key) {
            forward(successor(), askingNode.getIpAddress(), askingNode.getPort(), "lookup_reply", 0, 0, 0, null);
        } else {
            NodeProperties closest = closestPrecedingNode(key);

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties))
                forward(askingNode, closest.getIpAddress(), closest.getPort(), "lookup", 0, 0, key, null);
            else
                forward(properties, askingNode.getIpAddress(), askingNode.getPort(), "lookup_reply", 0, 0, 0, null);
        }
    }

    public void transferFiles(File[] allFiles) throws IOException {
        //System.out.println(allFiles.length);
        for (int i = 0; i < allFiles.length; i++) {
            File file = allFiles[i];
            String name = file.getName();
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream("./node" + this.getProperties().getNodeId() + "files/" + name);
                out = new FileOutputStream("./node" + this.getProperties().getNodeId() + "files/" + name + "Distributed");
                int c;
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }

            }
        }
    }


    /**
     * Cancel the timer that has been set before sending the request to check if the predecessor is still alive
     */
    void cancelCheckPredecessorTimer() {
        checkPredecessor.cancelTimer();
    }

    /**
     * Forward a request to a client
     *
     * @param targetNode is the {@code NodeProperties} information
     * @param ip         is the Ip address of the client to which to forward the request
     * @param port       is the port of the client to which to forward the request
     * @param msg        is the kind of request
     * @param key        is the hash of the key to search on the net
     */
    void forward(NodeProperties targetNode, String ip, int port, String msg, int fixId, int fixIndex, int key, File[] allFiles) {
        forwarder.makeRequest(targetNode, ip, port, msg, fixId, fixIndex, key, allFiles);
    }

    /**
     * Update and return the n_fix variable to properly run the fix_finger algorithm
     *
     * @return the index of the finger table to be used during the fix_finger algorithm
     */
    int nextFinger() {
        if (n_fix == KEY_SIZE - 1)
            n_fix = 0;
        else
            n_fix += 1;
        return n_fix;
    }

    /**
     * Update the finger table
     *
     * @param i       is the index of the table
     * @param newNode is the new value for the row with index i in the table
     */
    void updateFinger(int i, NodeProperties newNode) {
        // TODO: synchronized?
        fingers[i] = newNode;
    }

    void checkPredecessor() {
        if (predecessor != null) {
            forward(properties, predecessor.getIpAddress(), predecessor.getPort(), "check_predecessor", 0, 0, 0, null);
        }
    }

    boolean isPredecessorSet() {
        return predecessor != null;
    }

    void printServerCoordinates() {
        System.out.println("Server coordinates:");
        System.out.println("ID: " + properties.getNodeId());
        System.out.println("Ip: " + properties.getIpAddress());
        System.out.println("Port: " + properties.getPort());
        System.out.println("Coordinates: " + properties.getIpAddress() + ":" + properties.getPort());

        System.out.println("------------------------------------------\n");
    }

    void printFingerTable() {
        int limit = (int) Math.pow(2, KEY_SIZE);
        int bound;
        System.out.println("Finger table node id " + properties.getNodeId() + ":");
        System.out.println("i\tvalue\tbound");

        for (int i = 0; i < KEY_SIZE; i++) {
            if (fingers[i] != null) {
                bound = (int) (Math.pow(2, i) + properties.getNodeId()) % limit;
                System.out.println("[" + i + "]\t" + String.valueOf(fingers[i].getNodeId()) + "\t\t" + bound);
            } else {
                System.out.println("-");
            }
        }

        System.out.println("------------------------------------------\n");
    }

    void printPredecessorAndSuccessor() {

        System.out.println("Current node ID: " + properties.getNodeId());
        System.out.println();

        if (predecessor != null) {
            System.out.println("Predecessor coordinates:");
            System.out.println("ID: " + predecessor.getNodeId());
            System.out.println("Ip: " + predecessor.getIpAddress());
            System.out.println("Port: " + predecessor.getPort());
            System.out.println();
        }

        if (successor() != null) {
            System.out.println("Successor coordinates:");
            System.out.println("ID: " + successor().getNodeId());
            System.out.println("Ip: " + successor().getIpAddress());
            System.out.println("Port: " + successor().getPort());
        }

        System.out.println("------------------------------------------\n");
    }

    void printResources() {
        System.out.println("List of resources' keys contained by node " + properties.getNodeId() + ":");

        for (int resource : data.keySet()) {
            System.out.println(resource + ": " + data.get(resource).toString());
        }

        System.out.println("------------------------------------------\n");
    }

    public void close() {
        checkPredecessorThread.shutdownNow();
        fixFingersThread.shutdownNow();
        stabilizeThread.shutdownNow();
        nodeSocketServer.close();
    }
}

