package model;

import java.io.IOException;
import java.io.Serializable;
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
    void setPredecessor(NodeProperties predecessor) {
        this.predecessor = predecessor;
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
            e.printStackTrace();
        }

        assert currentIp != null;
        return currentIp.getHostAddress();
    }


    /**
     * Get the finger table of the current node
     *
     * @return an array of {@code NodeProperties}
     */
    NodeProperties[] getFingers() {
        return fingers;
    }

    /**
     * Update the predecessor node of the successor
     *
     * @param newNode is the node to be set as predecessor for the successor
     */

    void setSuccessorPredecessor(NodeProperties newNode) {
        synchronized (this) {
            stabilize.setSuccessorPredecessor(newNode);
            stabilize.cancelTimer();
        }
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
     * Create a new Chord Ring
     */
    void create() {

        startNode();
        startThreads();

        new Thread(new NodeSocketServer(this)).start();
    }

    /**
     * Join a Ring containing the known Node
     */
    void join(String ip, int port) {
        startNode();
        forward(properties, ip, port, "find_successor", 0, 0, 0);

        startThreads();
        new Thread(new NodeSocketServer(this)).start();

    }

    private void startNode() {
        serverSocket = createServerSocket();
        forwarder = new Forwarder();
        int port = serverSocket.getLocalPort();
        String ipAddress = getCurrentIp();

        initializeNode(ipAddress, port);

        System.out.println("The IP of this node is : " + ipAddress);
        System.out.println("The server is active on port " + port);
        System.out.println("The ID of the node is: " + getProperties().getNodeId());

    }

    private void initializeNode(String ipAddress, int port) {
        this.properties = new NodeProperties(sha1(ipAddress + ":" + port), ipAddress, port);
        this.setSuccessor(this.properties);
        this.predecessor = null;
/*
        for (int i = 0; i < KEY_SIZE; i++) {
            fingers[i] = properties;
        }
*/
    }


    private void startThreads() {
        checkPredecessorThread = Executors.newSingleThreadScheduledExecutor();
        fixFingersThread = Executors.newSingleThreadScheduledExecutor();
        stabilizeThread = Executors.newSingleThreadScheduledExecutor();

        checkPredecessorThread.scheduleAtFixedRate(checkPredecessor, 0, 6, TimeUnit.SECONDS);
        fixFingersThread.scheduleAtFixedRate(fixFingers, 2, 6, TimeUnit.SECONDS);
        stabilizeThread.scheduleAtFixedRate(stabilize, 4, 6, TimeUnit.SECONDS);
    }


    /**
     * Notify the current node that a new predecessor can exists fot itself
     *
     * @param predecessor node that could be the predecessor
     */
    void notifySuccessor(NodeProperties predecessor) {
        if ( (this.predecessor == null || predecessor.isInIntervalStrict(this.predecessor.getNodeId(), properties.getNodeId()))
                && !predecessor.equals(properties)) {
            setPredecessor(predecessor);
        }

    }

    /**
     * Find the successor of the node with the given id
     *
     * @param askingNode is the node that asked for findings its successor
     */
    void findSuccessor(NodeProperties askingNode) {

        if (askingNode.getNodeId() == properties.getNodeId())
            logger.log(Level.WARNING, "DHT incosistenti");


        if (askingNode.isInInterval(properties.getNodeId(), successor().getNodeId())) {
            System.out.println("Found------------------------------------------------");
            forwarder.makeRequest(successor(), askingNode.getIpAddress(), askingNode.getPort(), "find_successor_reply", 0, 0, 0);
        } else {
            System.out.println("Forward----------------------------------------------");
            NodeProperties newNodeToAsk = closestPrecedingNode(askingNode.getNodeId());

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!newNodeToAsk.equals(properties))
                forwarder.makeRequest(askingNode, newNodeToAsk.getIpAddress(), newNodeToAsk.getPort(), "find_successor", 0, 0, 0);
            else
                forwarder.makeRequest(properties, askingNode.getIpAddress(), askingNode.getPort(), "find_successor_reply", 0, 0, 0);
        }
        /*




            //If it's not the highest node in the net
            if (askingNode.getNodeId() > properties.getNodeId()) {

            //If the node is between the current node and its successor

            } else {

            }
        } else {
            System.out.println("4) This node is the successor. Asking node:" + askingNode.getNodeId());
            forwarder.makeRequest(properties, askingNode.getIpAddress(), askingNode.getPort(), "find_successor_reply", 0, 0, 0);
        }
        */
    }

    /**
     * Find the successor of the the askingNode to update its finger table
     *
     * @param askingNode is the node that has sent the first fix_finger request
     * @param fixId      is the upper bound Id of the fixIndex-th row of the finger table
     * @param fixIndex   is the index of the finger table to be updated
     */
    void fixFingerSuccessor(NodeProperties askingNode, int fixId, int fixIndex) {

        System.out.println("properties: " + properties.getNodeId());
        System.out.println("fixId: " + fixId);
        System.out.println("finger 0: " + successor().getNodeId());
       // System.out.println("fingers: " + fingers[fixIndex].getNodeId());


        if (NodeProperties.isInIntervalInteger(properties.getNodeId(), fixId, successor().getNodeId())) {
            System.out.println("Found------------------------------------------------");
            forward(successor(), askingNode.getIpAddress(), askingNode.getPort(), "fix_finger_reply", fixId, fixIndex, 0);
        } else {
            System.out.println("Forward----------------------------------------------");
            NodeProperties newNodeToAsk = closestPrecedingNode(askingNode.getNodeId());

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!newNodeToAsk.equals(properties)) {
                System.out.println("secondo if");
                forward(askingNode, newNodeToAsk.getIpAddress(), newNodeToAsk.getPort(), "fix_finger", fixId, fixIndex, 0);
            } else{
                System.out.println("secondo else");
                forward(properties, askingNode.getIpAddress(), askingNode.getPort(), "fix_finger_reply", fixId, fixIndex, 0);
            }
        }

        /*
        //If it's not the highest node in the net
        if ( fixId <= properties.getNodeId()) {

            //If the node is between the current node and its successor
            if (fixId > properties.getNodeId() && fixId <= fingers[0].getNodeId()) {
                System.out.println("Found------------------------------------------------");
                forwarder.makeRequest(fingers[0], askingNode.getIpAddress(), askingNode.getPort(), "fix_finger_reply", fixId, fixIndex, 0);
            } else {
                System.out.println("Forward----------------------------------------------");
                NodeProperties newNodeToAsk = closestPrecedingNode(fixId);

                //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
                if (!newNodeToAsk.equals(properties))
                    forwarder.makeRequest(askingNode, newNodeToAsk.getIpAddress(), newNodeToAsk.getPort(), "fix_finger",  fixId, fixIndex, 0);
                else
                    forwarder.makeRequest(fingers[0], askingNode.getIpAddress(), askingNode.getPort(), "fix_finger_reply",  fixId, fixIndex, 0);
            }
        } else {
            forwarder.makeRequest(fingers[0], askingNode.getIpAddress(), askingNode.getPort(), "fix_finger_reply",  fixId, fixIndex, 0);
        }


        if (fixIndex > properties.getNodeId() && fixIndex <= fingers[0].getNodeId()) {
            System.out.println("ENTRA");
            forwarder.makeRequest(fingers[0], askingNode.getIpAddress(), askingNode.getPort(), "fix_finger_reply", fixId, fixIndex, 0);
        } else {
            NodeProperties newNodeToAsk = closestPrecedingNode(askingNode.getNodeId());
            forwarder.makeRequest(askingNode, newNodeToAsk.getIpAddress(), newNodeToAsk.getPort(), "fix_finger", fixId, fixIndex, 0);
        }
*/

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
     * Look for the owner of a resource in the net
     *
     * @param key is the hash of the resource you're looking for in the net
     * @return the Ip address of the node that contains the resource, otherwise null
     */
    String lookup(int key) {
        // TODO: implement

        return null;
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
     * @param nodeInfo is the {@code NodeProperties} information
     * @param ip       is the Ip address of the client to which to forward the request
     * @param port     is the port of the client to which to forward the request
     * @param msg      is the kind of request
     * @param key      is the hash of the key to search on the net
     */
    void forward(NodeProperties nodeInfo, String ip, int port, String msg, int fixId, int fixIndex, int key) {
        forwarder.makeRequest(nodeInfo, ip, port, msg, fixId, fixIndex, key);
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

        System.out.println("Finger table of node " + properties.getNodeId());
        for (int j = 0; j < KEY_SIZE; j++) {
            if (fingers[j] != null) {
            }
            // System.out.println(fingers[j].getNodeId());
        }
    }

    void checkPredecessor() {
        if (predecessor != null) {
            System.out.println("Predecessor is " + predecessor.getNodeId());
            forward(properties, predecessor.getIpAddress(), predecessor.getPort(), "check_predecessor", 0, 0, 0);
        } else
            System.out.println("Predecessor is null");
    }

    boolean isPredecessorSet() {
        return predecessor != null;
    }
}

