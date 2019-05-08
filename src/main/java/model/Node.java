package model;

import network.Forwarder;
import network.NodeSocketServer;
import network.requests.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static model.NodeProperties.*;
import static utilities.Utilities.calculateFixId;
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

    public Forwarder getForwarder() {
        return forwarder;
    }

    /**
     * Contains information about the node
     */
    private NodeProperties properties;

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
    private ScheduledExecutorService forwarderThread;

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

        checkPredecessor = new CheckPredecessor(this);
        fixFingers = new FixFingers(this);
        stabilize = new Stabilize(this);
    }

    // Getter
    public NodeProperties getProperties() {
        return properties;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public NodeProperties getPredecessor() {
        return predecessor;
    }

    // Setter
    public void setPredecessor(NodeProperties predecessor) {

        this.predecessor = predecessor;

        /*if (predecessor == null) {
            this.predecessor = null;
            return;
        }

        //Each time the predecessor is updated, check if any resources should be forwarded to it
        if(!isPredecessorSet() || !this.predecessor.equals(predecessor)) {
            this.predecessor = predecessor;
            distributePredecessor();
        }*/


    }

    /**
     * Set the successor of the current node
     *
     * @param node the successor
     */
    public void setSuccessor(NodeProperties node) {
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
    public void finalizeStabilize(NodeProperties newNode) {
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
        forwarder.makeRequest(ip, port, new FindSuccessorRequest(properties));

        startThreads();
        nodeSocketServer = new NodeSocketServer(this);

        new Thread(nodeSocketServer).start();
    }

    public boolean isNodeAlone() {
        if (predecessor.equals(properties)) {
            if (properties.equals(successor())) {
                return true;
            }
        }
        return false;
    }

    public void publishResources() {

        //case in which you're the only node in the network, so your files are moved from the offline folder to the online one;
        if (isNodeAlone()) {
            File folder = new File("./node" + this.getProperties().getNodeId() + "/offline");
            File[] allFiles = folder.listFiles();
            for (File file : allFiles) {
                saveFile(file);
                file.delete();
            }
        }
        //common case, resources forwarded to others
        else {
            File folder = new File("./node" + this.getProperties().getNodeId() + "/offline");
            File[] allFiles = folder.listFiles();
            for (File file : allFiles) {
                forwarder.makeRequest(successor().getIpAddress(), successor().getPort(), new DistributeResourceRequest(null, file));
                file.delete();
            }
        }
        System.out.println("You correctly published your resources! Some of them could have been forwarded to other nodes, while some could still be of your property placed in your online folder");

    }

    public void askSuccessorForResources() {
        forwarder.makeRequest(successor().getIpAddress(), successor().getPort(), new AskSuccessorResourcesRequest(this.getProperties()));
    }

    public void giveResourcesToPredecessor(NodeProperties nodeProperties) {
        File folder = new File("./node" + this.getProperties().getNodeId() + "/online");
        File[] allFiles = folder.listFiles();
        for (int i = 0; i < allFiles.length; i++) {
            File f = allFiles[i];
            if (checkResourcesForPredecessor(sha1(allFiles[i].getName()), nodeProperties.getNodeId(), properties.getNodeId())) {
                forwarder.makeRequest(nodeProperties.getIpAddress(), nodeProperties.getPort(), new DistributeResourceRequest(nodeProperties, f));
                f.delete();
            }
        }
    }

    private void startNode() {
        serverSocket = createServerSocket();
        int port = serverSocket.getLocalPort();
        String ipAddress = getCurrentIp();
        initializeNode(ipAddress, port);
        forwarder = new Forwarder(properties.getNodeId());
        foldersCreation();
    }

    private void foldersCreation() {
        File f = new File("./node" + properties.getNodeId() + "/offline/offlineFolderCreation");
        if (!f.getParentFile().exists())
            f.getParentFile().mkdirs();
        File g = new File("./node" + this.getProperties().getNodeId() + "/online/" + "onlineFolderCreation");
        if (!g.getParentFile().exists())
            g.getParentFile().mkdirs();
    }

    private void initializeNode(String ipAddress, int port) {
        this.properties = new NodeProperties(sha1(ipAddress + ":" + port), ipAddress, port);
        setSuccessor(this.properties);
        this.predecessor = null;
    }


    public void notifyNeighbours() {
        forwarder.makeRequest(predecessor.getIpAddress(), predecessor.getPort(), new UpdateSuccessorRequest(successor()));
        forwarder.makeRequest(successor().getIpAddress(), successor().getPort(), new UpdatePredecessorRequest(getPredecessor()));
    }


    private void startThreads() {
        checkPredecessorThread = Executors.newSingleThreadScheduledExecutor();
        fixFingersThread = Executors.newSingleThreadScheduledExecutor();
        stabilizeThread = Executors.newSingleThreadScheduledExecutor();
        forwarderThread = Executors.newSingleThreadScheduledExecutor();

        forwarderThread.scheduleAtFixedRate(forwarder, 1, CHECK_SOCKET_PERIOD, TimeUnit.MILLISECONDS);
        checkPredecessorThread.scheduleAtFixedRate(checkPredecessor, 0, CHECK_PERIOD, TimeUnit.MILLISECONDS);
        fixFingersThread.scheduleAtFixedRate(fixFingers, 200, FIX_PERIOD, TimeUnit.MILLISECONDS);
        stabilizeThread.scheduleAtFixedRate(stabilize, 400, STABILIZE_PERIOD, TimeUnit.MILLISECONDS);
    }


    /**
     * Notify the current node that a new predecessor can exists fot itself
     *
     * @param predecessor node that could be the predecessor
     */
    public void notifySuccessor(NodeProperties predecessor) {
        if ((this.predecessor == null || predecessor.isInIntervalStrict(this.predecessor.getNodeId(), properties.getNodeId()))) {
            setPredecessor(predecessor);
        }
    }

    /**
     * Find the successor of the node with the given id
     *
     * @param askingNode is the node that asked for findings its successor
     */
    public void findSuccessor(NodeProperties askingNode) {

        // check if there are two nodes with the same Id
        if (askingNode.getNodeId() == properties.getNodeId())
            logger.log(Level.SEVERE, "inconsistency: two nodes with the same ID");

        if (askingNode.isInInterval(properties.getNodeId(), successor().getNodeId())) {
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(), new FindSuccessorReplyRequest(successor()));
        } else {
            NodeProperties closest = closestPrecedingNode(askingNode.getNodeId());

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties))
                forwarder.makeRequest(closest.getIpAddress(), closest.getPort(), new FindSuccessorRequest(askingNode));
            else
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(), new FindSuccessorReplyRequest(properties));
        }
    }

    /**
     * Find the successor of the the askingNode to update its finger table
     *
     * @param askingNode is the node that has sent the first fix_finger request
     * @param fixId      is the upper bound Id of the fixIndex-th row of the finger table
     * @param fixIndex   is the index of the finger table to be updated
     */
    public void fixFingerSuccessor(NodeProperties askingNode, int fixId, int fixIndex) {

        if (NodeProperties.isInIntervalInteger(properties.getNodeId(), fixId, successor().getNodeId())) {
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(), new FixFingerReplyRequest(successor(), fixId, fixIndex));
        } else {
            NodeProperties closest = closestPrecedingNode(fixId);

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties)) {
                forwarder.makeRequest(closest.getIpAddress(), closest.getPort(), new FixFingerRequest(askingNode, fixId, fixIndex));
            } else {
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(), new FixFingerReplyRequest(properties, fixId, fixIndex));
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
    public void lookup(NodeProperties askingNode, int key) {

        if (isInIntervalInteger(properties.getNodeId(), key, successor().getNodeId())) {
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(), new LookupReplyRequest(successor(), key));
        } else {
            NodeProperties closest = closestPrecedingNode(key);

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties))
                forwarder.makeRequest(closest.getIpAddress(), closest.getPort(), new LookupRequest(askingNode, key));
            else
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(), new LookupReplyRequest(properties, key));
        }
    }

    public void saveFile(File file) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream("./node" + properties.getNodeId() + "/online/" + file.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Cancel the timer that has been set before sending the request to check if the predecessor is still alive
     */
    public void cancelCheckPredecessorTimer() {
        checkPredecessor.cancelTimer();
    }

    /**
     * Forward a request to a client
     *
     * param targetNode is the {@code NodeProperties} information
     * param ip         is the Ip address of the client to which to forward the request
     * param port       is the port of the client to which to forward the request
     * param msg        is the kind of request
     * param key        is the hash of the key to search on the net
     */


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
    public void updateFinger(int i, NodeProperties newNode) {
        synchronized (fingers) {
            fingers[i] = newNode;
        }
    }

    void checkPredecessor() {
        forwarder.makeRequest(predecessor.getIpAddress(), predecessor.getPort(), new CheckPredecessorRequest(properties));
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

    /**
     * Terminate all the threads and close the socket connection of the server side
     */
    public void close() {
        checkPredecessorThread.shutdownNow();
        fixFingersThread.shutdownNow();
        stabilizeThread.shutdownNow();
        forwarderThread.shutdownNow();
        forwarder.stop();
        nodeSocketServer.close();
        forwarder.stop();
    }

    /**
     * Send the files to be assigned to your predecessor
     */
    /*
    public void distributePredecessor() {

        System.out.println("___________________________________________________________________________________________________________________PREDECESSOR______________");
        File folder = new File("./node" + properties.getNodeId());
        File[] allFiles = folder.listFiles();

        for (File file : allFiles) {
            if (isPredecessorSet() && !isInIntervalInteger(predecessor.getNodeId(), sha1(file.getName()), properties.getNodeId())) {
                System.out.println("File " + sha1(file.getName()) + " to " + predecessor.getNodeId());
                sendResource(predecessor.getIpAddress(), predecessor.getPort(), "file_to_predecessor", file);
                file.delete();
            }
        }
    }*/
    public void distributeResource(NodeProperties nodeProperties, File file) {

        //called when resources are published on purpose
        if (nodeProperties == null) {
            int fileId = sha1(file.getName());

            // if the resource must be kept
            if (isPredecessorSet() && isInIntervalInteger(predecessor.getNodeId(), sha1(file.getName()), properties.getNodeId())) {
                saveFile(file);
                return;
            }

            //Search for the highest finger available
            int highestIndex = searchHighestFinger();

            // if the resource must be sent to the successor
            if (isInIntervalInteger(properties.getNodeId(), fileId, successor().getNodeId())) {
                /*System.out.println("___________________________________________________________________________________________________________________DISTRIBUTE SUCCESSOR______________");
                System.out.println("File " + fileId + " to " + successor().getNodeId());*/
                forwarder.makeRequest(successor().getIpAddress(), successor().getPort(), new DistributeResourceRequest(null, file));
                return;
            }

            // if the resource must be sent to one of the fingers
            for (int i = 0; i < KEY_SIZE - 1; i++) {
                int lowerBound = calculateFixId(properties.getNodeId(), i);
                int upperBound = calculateFixId(properties.getNodeId(), i + 1);

                if (i + 1 <= highestIndex && isInIntervalInteger(lowerBound, fileId, upperBound)) {
                    /*System.out.println("___________________________________________________________________________________________________________________DISTRIBUTE FOR______________");
                    System.out.println("File " + fileId + " to " + fingers[i + 1].getNodeId());*/
                    forwarder.makeRequest(fingers[i + 1].getIpAddress(), fingers[i + 1].getPort(), new DistributeResourceRequest(null, file));
                    return;
                }

            }

            // if the resource is out of the scope of the finger table forward it to the last finger, but only if it's not yourself
            if (fingers[highestIndex].getNodeId() != properties.getNodeId()) {
                /*System.out.println("___________________________________________________________________________________________________________________DISTRIBUTE LAST______________");
                System.out.println("File " + fileId + " to " + fingers[highestIndex].getNodeId());*/
                forwarder.makeRequest(fingers[highestIndex].getIpAddress(), fingers[highestIndex].getPort(), new DistributeResourceRequest(null, file));
            } else {
                saveFile(file); // temporarily save the file
            }
        }

        //called when a new node ask the successor for its resources, so it receives them and saves them
        else {
            saveFile(file);
        }
    }

    // return the highest non null finger index
    private int searchHighestFinger() {
        for (int i = KEY_SIZE - 1; i >= 0; i--) {
            if (fingers[i] != null) {
                return i;
            }
        }
        return 0;
    }

    // TODO: concurrent access to files by different nodes on the same machine if enabled
    // check if you must send some resources to other nodes
    /*public void checkResources() {
        try {
            File folder = new File("./node" + properties.getNodeId());
            File[] allFiles = folder.listFiles();

            for (File file : allFiles) {
                int fileId = sha1(file.getName());
                int highestIndex = searchHighestFinger();

                if (!isPredecessorSet() || !isInIntervalInteger(predecessor.getNodeId(), fileId, properties.getNodeId())) {

                    // if the resource must be sent to one of the fingers
                    for (int i = 0; i < KEY_SIZE - 1; i++) {

                        int lowerBound = calculateFixId(properties.getNodeId(), i);
                        int upperBound = calculateFixId(properties.getNodeId(), i + 1);

                        if (i + 1 <= highestIndex && isInIntervalInteger(lowerBound, fileId, upperBound) && fileId <= fingers[i + 1].getNodeId()) {
                            sendResource(fingers[i + 1].getIpAddress(), fingers[i + 1].getPort(), "distribute_resource", file);
                            file.delete();
                            return;
                        }
                    }
                }

                // if the resource is out of the scope of the finger table forward it to the last finger, but only if it's not yourself
                if (fingers[highestIndex].getNodeId() != properties.getNodeId()) {
                    sendResource(fingers[highestIndex].getIpAddress(), fingers[highestIndex].getPort(), "distribute_resource", file);
                    file.delete();
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Trying to concurrently access files");
        }
    }*/

    public void transferOnLeave() {
        File folder = new File("./node" + properties.getNodeId() + "/online");
        File[] allFiles = folder.listFiles();

        // TODO: after the switch to the Visitor pattern, send them as a list
        for (File file : allFiles) {
            forwarder.makeRequest(successor().getIpAddress(), successor().getPort(), new TransferAfterLeaveRequest(file));
            file.delete();
        }
    }
}

