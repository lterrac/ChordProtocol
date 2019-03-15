package main.java.model;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static utilities.HashText.sha1;

public class Node {

    private static final Logger logger = Logger.getLogger(Node.class.getName());

    /**
     * Finger table of the node
     */
    private final NodeProperties[] fingers = new NodeProperties[NodeProperties.KEY_SIZE];

    /**
     * Contains information about the node
     */
    private NodeProperties properties;

    /**
     * Data contained in the node
     */
    private HashMap<Integer, Serializable> data = new HashMap<>();

    /**
     * List of adjacent successors of the node
     */
    private List<NodeProperties> successors = new ArrayList<>();

    private NodeProperties predecessor;

    private ServerSocket serverSocket;

    /**
     * Create a new Chord Ring
     */
    public Node() {
        serverSocket = createServerSocket();

        //Find Ip address, it will be published later for joining
        InetAddress currentIp = null;
        try {
            currentIp = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        assert currentIp != null;
        String ipAddress = currentIp.getHostAddress();

        logger.log(Level.INFO, "IP of this node is := " + ipAddress);

        //Create node information
        this.properties = new NodeProperties(sha1(ipAddress), ipAddress, serverSocket.getLocalPort());
        this.successor(this.properties);
        this.predecessor = null;

        new Thread(new HandleRequest(this)).start();
    }

    /**
     * Join a Ring containing the known Node
     */
    public Node(String ipAddress, int port) {



        /*
        //TODO Decide what port use
        //int port = 0;

        //Find Ip address, it will be published later for joining
        InetAddress ipAddress= null;
        try {
            ipAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (ipAddress != null) {
            System.out.println("IP of this node is := "+ipAddress.getHostAddress());
        } else {
            System.err.println("There is no ip address!");
            return;
        }

        //Create node information
        this.properties = new NodeProperties(sha1(ipAddress.getHostAddress()), ipAddress.getHostAddress(), port);

        NodeProperties successor = null;
         TODO Decide if methods will be sync or async.
           sync: put the class in wait until a variable (successor) is set
           async: split the method in the paper in two methods

        this.successor(successor);
        this.predecessor = null;
        */

    }

    /**
     * Set the successor of the current node
     *
     * @param node the successor
     */
    private void successor(NodeProperties node) {
        synchronized (this.fingers) {
            this.fingers[0] = node;
        }
    }

    //TODO: Will go into a thread

    /**
     * Veriﬁes n’s immediate successor, and tells the successor about n.
     */
    public void stabilize() {

    }

    /**
     * @param predecessor node that could be the predecessor
     */
    public void notifySuccessor(Node predecessor) {

    }

    //TODO: Will go into a thread

    /**
     * Refresh fingers table
     */
    public void fixFingers() {

    }

    //TODO: Will go into a thread

    /**
     * Check if predecessor has failed
     */
    public void checkPredecessor() {

    }

    /**
     * Find the successor of the node with the given id
     *
     * @param nodeId
     */
    public void findSuccessor(int nodeId) {

    }

    /**
     * Find the highest predecessor of id
     *
     * @param nodeId
     */
    public void closestPrecedingNode(int nodeId) {

    }

    public NodeProperties getProperties() {
        return properties;
    }

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

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
}
