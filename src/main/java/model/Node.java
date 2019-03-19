package model;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static model.NodeProperties.KEY_SIZE;
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

    private final ExecutorService pool;

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

    private ServerSocket serverSocket;

    public Node() {
        successors = new ArrayList<>();
        data = new HashMap<>();
        pool = Executors.newScheduledThreadPool(3);

        checkPredecessor = new CheckPredecessor(this);
        fixFingers = new FixFingers();
        stabilize = new Stabilize(this);
    }

    // Getter
    public NodeProperties getProperties() {
        return properties;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public NodeProperties getPredecessor(){
        return predecessor;
    }

    // Setter
    public void setPredecessor(NodeProperties predecessor) {
        this.predecessor = predecessor;
    }

    /**
     * Create a new Chord Ring
     */
    public void create() {
        serverSocket = createServerSocket();
        forwarder = new Forwarder();
        int port = serverSocket.getLocalPort();
        String ipAddress = getCurrentIp();

        System.out.println("The IP of this node is : " + ipAddress);
        System.out.println("The server is active on port " + port);

        //Create node information
        this.properties = new NodeProperties(sha1(ipAddress + ":" + port), ipAddress, port);
        this.setSuccessor(this.properties);
        this.predecessor = null;

        startThreads();

        new Thread(new NodeSocketServer(this)).start();
    }

    /**
     * Join a Ring containing the known Node
     */
    public void join(String ip, int port) {

        serverSocket = createServerSocket();
        forwarder = new Forwarder();
        int newPort = serverSocket.getLocalPort();
        String newIp = getCurrentIp();

        this.properties = new NodeProperties(sha1(newIp + ":" + newPort), newIp, newPort);
        this.predecessor = null;

        /*
        Debug line: try the connection with a ping message

        forwarder.makeRequest(ip, port, "ping");

         */

        //TODO Put into a thread otherwise it won't be possible to handle other requests until the response is received!
        forwarder.makeRequest(properties, ip,port,"find_successor");

        //TODO What is this PAAAAAAAAAAAAAAAOOOOOOOOOOLOOOOOOOOOOOOOO
        //this.successors.remove(0);
        //this.successors.add(0, findSuccessor(properties.getNodeId()));

        //NodeProperties successor = forwarder.makeRequest(ip, port, "find_successor:" + properties.getNodeId());

        startThreads();
    }

    private void startThreads() {
        checkPredecessorThread.scheduleAtFixedRate(checkPredecessor,0,6, TimeUnit.SECONDS);
        fixFingersThread.scheduleAtFixedRate(fixFingers,2,6, TimeUnit.SECONDS);
        stabilizeThread.scheduleAtFixedRate(stabilize,4,6, TimeUnit.SECONDS);
    }

    /**
     * Set the successor of the current node
     *
     * @param node the successor
     */
    public void setSuccessor(NodeProperties node) {
        synchronized (this.fingers) {
            this.fingers[0] = node;
        }
    }

    /**
     * Notify the current node that a new predecessor can exists fot itself
     *
     * @param predecessor node that could be the predecessor
     */
    public void notifySuccessor(NodeProperties predecessor) {
        if (this.predecessor == null ||
                predecessor.isInIntervalStrict(this.predecessor.getNodeId(), predecessor.getNodeId())) {
            this.predecessor = predecessor;
        }
    }

    /**
     * Find the successor of the node with the given id
     *
     * @param askingNode
     */
    public void findSuccessor(NodeProperties askingNode) {
        if (askingNode.isInInterval(properties.getNodeId(),fingers[0].getNodeId()))
            forwarder.makeRequest(fingers[0],askingNode.getIpAddress(),
                    askingNode.getPort(), "found_successor");
        else {
            NodeProperties newNodeToAsk = closestPrecedingNode(askingNode);
            forwarder.makeRequest(newNodeToAsk, askingNode.getIpAddress(),askingNode.getPort(),"find_successor");
        }
    }

    /**
     * Find the highest predecessor of id.
     *
     * @param nodeId
     * @return
     */
    public NodeProperties closestPrecedingNode(NodeProperties nodeId) {
        for (int i = 0; i < KEY_SIZE; i++) {
            if (fingers[i].isInInterval(properties.getNodeId(), nodeId.getNodeId()))
                return fingers[i];
        }
        return properties;
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
            e.printStackTrace();
        }

        return serverSocket;
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
            e.printStackTrace();
        }

        assert currentIp != null;
        return currentIp.getHostAddress();
    }

    /**
     * Look for the owner of a resource in the net
     *
     * @param key is the hash of the resource you're looking for in the net
     * @return the Ip address of the node that contains the resource, otherwise null
     */
    public String lookup(int key) {
        // TODO: implement

        return null;
    }

    /**
     * Get the finger table of the current node
     *
     * @return an array of {@code NodeProperties}
     */
    public NodeProperties[] getFingers() {
        return fingers;
    }

    public Stabilize getStabilize() {
        return stabilize;
    }

    /**
     * Cancel the timer that has been set before sending the request to check if the predecessor is still alive
     */
    public void cancelCheckPredecessorTimer(){
        checkPredecessor.cancelTimer();
    }

    /**
     * Forward a request to a client
     *
     * @param nodeInfo is the {@code NodeProperties} information
     * @param ip is the Ip address of the client to which to forward the request
     * @param port is the port of the client to which to forward the request
     * @param msg is the kind of request
     */
    public void forward(NodeProperties nodeInfo, String ip, int port, String msg){
        forwarder.makeRequest(nodeInfo, ip, port, msg);
    }
}
