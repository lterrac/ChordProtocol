package model;

import network.Forwarder;
import network.NodeSocketServer;
import network.ping.PingPredecessor;
import network.ping.PingServer;
import network.ping.PingSuccessor;
import network.requests.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static model.NodeProperties.*;
import static utilities.Utilities.sha1;

/**
 * Main class of the Chord protocol.
 */
public class Node {

    private static final Logger logger = Logger.getLogger(Node.class.getName());

    /**
     * Finger table of the node
     */
    private final NodeProperties[] fingers = new NodeProperties[KEY_SIZE];
    /**
     * UDP ping implementation
     */

    private final Object nodeLock;
    private final Object predecessorLock;
    private final Object fingersLock;
    private final Object successorsLock;
    private final Object fileLock;
    /**
     * Represents the "client side" of a node. It sends requests to other nodes
     */
    private Forwarder forwarder;
    /**
     * Contains information about the node
     */
    private NodeProperties properties;
    /**
     * List of adjacent successors of the node
     */
    private Deque<NodeProperties> successors;
    /**
     * Scheduled executor to run threads at regular time intervals
     */
    private ScheduledExecutorService fixFingersThread;
    private ScheduledExecutorService stabilizeThread;
    private ScheduledExecutorService forwarderThread;
    /**
     * Classes containing the threads code
     */
    private FixFingers fixFingers;
    private Stabilize stabilize;
    private NodeProperties predecessor;
    /**
     * Socket server for accepting new socket connections
     */
    private NodeSocketServer nodeSocketServer;
    private ServerSocket serverSocket;
    private PingServer pingSuccessorServer;
    private PingServer pingPredecessorServer;
    private PingSuccessor pingSuccessor;
    private PingPredecessor pingPredecessor;

    /**
     * Useful to save the index of the finger table to which to apply the fix_finger algorithm
     */
    private int n_fix;

    public Node() {
        n_fix = -1;
        successors = new ArrayDeque<>(KEY_SIZE);
        fixFingers = new FixFingers(this);
        stabilize = new Stabilize(this);

        nodeLock = new Object();
        predecessorLock = new Object();
        successorsLock = new Object();
        fingersLock = new Object();
        fileLock = new Object();
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

    public void setPredecessor(NodeProperties predecessor) {
        synchronized (predecessorLock) {
            this.predecessor = predecessor;

            if (pingPredecessor != null) {
                pingPredecessor.terminate();
                logger.log(Level.INFO, "stopped pingPredecessor client");
            }


            if (predecessor != null) {
                logger.log(Level.INFO, "A node crashed. I'm building another pingPredecessor client for predecessor " + predecessor.getNodeId());
                pingPredecessor = new PingPredecessor(this, predecessor.getIpAddress(), predecessor.getUdpPredecessorServerPort(), predecessor.getNodeId());
                new Thread(pingPredecessor).start();

                deleteBackupFolderAndRecreate();
                askPredecessorForBackupResources();
            } else {
                pingPredecessor = null;

                transferBackupsToOnline();
            }
        }
    }

    /**
     * Updates the successor list of the current node
     */
    void updateSuccessors(Deque<NodeProperties> sList) {
        synchronized (successorsLock) {
            successors = sList;
        }
    }

    /**
     * TODO
     */
    public Deque<NodeProperties> getCustomizedSuccessors() {
        synchronized (successorsLock) {
            Deque<NodeProperties> newList = new ArrayDeque<>(successors);

            if (newList.size() >= KEY_SIZE) {
                newList.removeLast();
            }
            newList.addFirst(properties);

            return newList;
        }
    }

    /**
     * Replace the successor after having detected that has crashed. The new successor will be the next in the successors list
     */
    public void replaceSuccessor() {

        System.out.println("replace successor");

        synchronized (fingers[0]) {
            fingers[0] = successors.removeFirst();
            System.out.println("Now successor is " + successor().getNodeId());
        }

        //stop the old pinger if exists
        if (pingSuccessor != null) {
            pingSuccessor.terminate();
            logger.log(Level.INFO, "stopped pingSuccessor client");
        }

        //create a new one only if I am not the last node left in the network
        if (successor().getNodeId() != properties.getNodeId()) {
            logger.log(Level.INFO, "A node crashed. I'm building another pingSuccessor client for successor " + successor().getNodeId());
            NodeProperties successor = successor();
            pingSuccessor = new PingSuccessor(this, successor.getIpAddress(), successor.getUdpSuccessorServerPort(), successor.getNodeId());
            new Thread(pingSuccessor).start();
        }
    }

    public Forwarder getForwarder() {
        return forwarder;
    }


    // Setter

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
     * Check if {@link #predecessor} is set
     *
     * @return true if it is not null, otherwise false
     */
    boolean isPredecessorSet() {
        return predecessor != null;
    }

    /**
     * Get the finger 0 of the finger table
     *
     * @return Node successor
     */
    public NodeProperties successor() {
        return fingers[0];
    }

    /**
     * Set the successor of the current node
     *
     * @param node the successor
     */
    public void setSuccessor(NodeProperties node) {

        System.out.println("set successor");

        synchronized (fingers) {
            this.fingers[0] = node;
        }

        if (pingSuccessor != null) {
            pingSuccessor.terminate();
            logger.log(Level.INFO, "stopped pingSuccessor client");
        }

        // restart the ping client every time the successor changes
        if (successor().getNodeId() != this.properties.getNodeId()) {
            logger.log(Level.INFO, "Successor changed. I'm building another pingSuccessor client for successor " + successor().getNodeId());
            NodeProperties successor = successor();
            pingSuccessor = new PingSuccessor(this, successor.getIpAddress(), successor.getUdpSuccessorServerPort(), successor.getNodeId());
            new Thread(pingSuccessor).start();
        }

    }

    /**
     * Update the predecessor node of the successor in the stabilize thread and the successors list
     *
     * @param newNode is the node to be set as predecessor for the successor
     */
    public void finalizeStabilize(NodeProperties newNode, Deque<NodeProperties> successors) {
        stabilize.finalizeStabilize(newNode, successors);
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
    public void create() {

        startNode();

        startThreads();

        nodeSocketServer = new NodeSocketServer(this);

        new Thread(nodeSocketServer).start();
    }

    /**
     * Join a Ring containing the known Node
     */
    public void join(String ip, int port) {
        startNode();
        forwarder.makeRequest(ip, port, new FindSuccessorRequest(properties));

        startThreads();
        nodeSocketServer = new NodeSocketServer(this);

        new Thread(nodeSocketServer).start();
    }

    /**
     * Check if the current node is the last in the network
     *
     * @return true if it is the last one, otherwise false
     */
    private boolean isNodeAlone() {
        return successor().equals(properties);
    }

    /**
     * Pubblish the resources into the network.
     * It moves them from the folder "/offline" to "/online"
     */
    public void publishResources() {
        //case in which you're the only node in the network, so your files are moved from the offline folder to the online one;
        if (isNodeAlone()) {
            File folder = new File("./node" + properties.getNodeId() + "/offline");
            File[] allFiles = folder.listFiles();
            for (File file : allFiles) {
                saveFile(file, "online");
                file.delete();
            }
        }
        //common case, resources forwarded to others
        else {
            File folder = new File("./node" + properties.getNodeId() + "/offline");
            File[] allFiles = folder.listFiles();
            for (File file : allFiles) {
                forwarder.makeRequest(successor().getIpAddress(), successor().getTcpServerPort(), new LookupRequest(properties, sha1(file.getName()), true, file));
                file.delete();
            }
        }
        System.out.println("You correctly published your resources! Some of them could have been forwarded to other nodes, while some could still be of your property placed in your online folder");
    }

    /**
     * Ask to the successor if it holds some resources that needs to be managed by the current node
     */
    public void askSuccessorForResources() {
        forwarder.makeRequest(successor().getIpAddress(), successor().getTcpServerPort(), new AskSuccessorResourcesRequest(properties));
    }

    /**
     * Check if some file needs to be managed by the predecessor and in case sends the files back and deletes its copy.
     * The predecessor is passed as a parameter because, when a node joins the network, {@link #predecessor} may not
     * be already set by the {@link #notifySuccessor(NodeProperties)}
     *
     * @param nodeProperties predecessor of the node
     */
    public void giveResourcesToPredecessor(NodeProperties nodeProperties) {
        File folder = new File("./node" + properties.getNodeId() + "/online");
        File[] allFiles = folder.listFiles();
        for (File f : allFiles) {
            if (checkResourcesForPredecessor(sha1(f.getName()), nodeProperties.getNodeId(), properties.getNodeId())) {
                forwarder.makeRequest(nodeProperties.getIpAddress(), nodeProperties.getTcpServerPort(), new DistributeResourceRequest(f, false));
                forwarder.makeRequest(successor().getIpAddress(), successor().getTcpServerPort(), new TellSuccessorToDeleteBackupRequest(f));
                f.delete();
            }
        }
    }

    /**
     * The current node transfers its backup resources to the online folder
     */
    private void transferBackupsToOnline() {
        //Move from backup to online
        System.out.println("save file because the predecessor is null");
        File folder = new File("./node" + this.properties.getNodeId() + "/backup");
        File[] allFiles = folder.listFiles();
        for (File file : allFiles) {
            saveFile(file, "online");
            forwarder.makeRequest(successor().getIpAddress(), successor().getTcpServerPort(), new DistributeResourceRequest(file, true));
            file.delete();
        }
    }

    /**
     * The current node asks the predecessor for its online resources to put them in its backup folder
     */
    public void askPredecessorForBackupResources() {
        System.out.println("ask to predecessor " + sha1(predecessor.getIpAddress() + ":" + predecessor.getTcpServerPort()) + "for backup resources");
        forwarder.makeRequest(predecessor.getIpAddress(), predecessor.getTcpServerPort(), new AskPredecessorBackupResourcesRequest(properties));
    }

    /**
     * Creates the primary classes of the node.
     * Create : - ServerSocket to accept incoming connections
     * - Set
     */
    private void startNode() {
        serverSocket = createServerSocket();
        int tcpServerPort = serverSocket.getLocalPort();
        String ipAddress = getCurrentIp();
        pingSuccessorServer = new PingServer();
        pingPredecessorServer = new PingServer();
        new Thread(pingSuccessorServer).start();
        new Thread(pingPredecessorServer).start();
        int udpSuccessorServerPort = pingSuccessorServer.getPort();
        int udpPredecessorPort = pingPredecessorServer.getPort();
        initializeNode(ipAddress, tcpServerPort, udpSuccessorServerPort, udpPredecessorPort);
        forwarder = new Forwarder(properties.getNodeId());
        foldersCreation();
    }

    /**
     * Create the folders "/online" and "/offline" for the node in which its resources will be stored
     */
    private void foldersCreation() {
        File f = new File("./node" + properties.getNodeId() + "/offline/offlineFolderCreation");
        if (!f.getParentFile().exists())
            f.getParentFile().mkdirs();
        File g = new File("./node" + properties.getNodeId() + "/online/" + "onlineFolderCreation");
        if (!g.getParentFile().exists())
            g.getParentFile().mkdirs();
        File h = new File("./node" + properties.getNodeId() + "/backup/backupFolderCreation");
        if (!h.getParentFile().exists())
            h.getParentFile().mkdirs();
    }

    /**
     * Initialize the attributes of the node:
     * - Creates the {@link NodeProperties} of the node
     * - Set the {@link #successor} to the node itself
     * - Set the {@link #predecessor} to null
     *
     * @param ipAddress     IP of the node
     * @param tcpServerPort Port on which the node is listening
     */
    private void initializeNode(String ipAddress, int tcpServerPort, int udpSuccessorServerPort, int udpPredecessorServerPort) {
        this.properties = new NodeProperties(sha1(ipAddress + ":" + tcpServerPort), ipAddress, tcpServerPort, udpSuccessorServerPort, udpPredecessorServerPort);
        setSuccessor(this.properties);
        this.predecessor = null;
    }

    /**
     * Starts the routines :
     * <p>
     * - Check Predecessor
     * - Fix Fingers
     * - Stabilize
     * - Forwarder (check for unused sockets)
     */
    private void startThreads() {
        fixFingersThread = Executors.newSingleThreadScheduledExecutor();
        stabilizeThread = Executors.newSingleThreadScheduledExecutor();
        forwarderThread = Executors.newSingleThreadScheduledExecutor();

        forwarderThread.scheduleAtFixedRate(forwarder, 1, CHECK_SOCKET_PERIOD, TimeUnit.MILLISECONDS);
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
            System.out.println("notify predecessor " + predecessor.getNodeId());
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
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getTcpServerPort(), new FindSuccessorReplyRequest(successor()));
        } else {
            NodeProperties closest = closestPrecedingNode(askingNode.getNodeId());

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties))
                forwarder.makeRequest(closest.getIpAddress(), closest.getTcpServerPort(), new FindSuccessorRequest(askingNode));
            else
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getTcpServerPort(), new FindSuccessorReplyRequest(properties));
        }
    }

    /**
     * Find the successor of the the askingNode to update its finger table.
     * Version adapted for Fix Fingers thread
     *
     * @param askingNode is the node that has sent the first fix_finger request
     * @param fixId      is the upper bound Id of the fixIndex-th row of the finger table
     * @param fixIndex   is the index of the finger table to be updated
     */
    public void fixFingerSuccessor(NodeProperties askingNode, int fixId, int fixIndex) {

        if (NodeProperties.isInIntervalInteger(properties.getNodeId(), fixId, successor().getNodeId())) {
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getTcpServerPort(), new FixFingerReplyRequest(successor(), fixId, fixIndex));
        } else {
            NodeProperties closest = closestPrecedingNode(fixId);

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties)) {
                forwarder.makeRequest(closest.getIpAddress(), closest.getTcpServerPort(), new FixFingerRequest(askingNode, fixId, fixIndex));
            } else {
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getTcpServerPort(), new FixFingerReplyRequest(properties, fixId, fixIndex));
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

        NodeProperties closest = properties;
        boolean found = false;

        // first check the finger table
        for (int i = KEY_SIZE - 1; i >= 0 && !found; i--) {
            if (fingers[i] != null && fingers[i].isInInterval(properties.getNodeId(), nodeId)) {
                closest = fingers[i];
                found = true;
            }
        }

        // then check the successors list
        for (NodeProperties n : successors) {
            if (n.getNodeId() > closest.getNodeId() && n.getNodeId() < nodeId) {
                closest = n;
            }
        }
        return closest;
    }


    /**
     * Look for the owner of a resource in the net.
     *
     * @param key is the hash of the resource you're looking for in the net
     */
    public void lookup(NodeProperties askingNode, int key, boolean transfer, File file) {

        if (isInIntervalInteger(properties.getNodeId(), key, successor().getNodeId())) {
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getTcpServerPort(), new LookupReplyRequest(successor(), key, transfer, file));
        } else {
            NodeProperties closest = closestPrecedingNode(key);

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.equals(properties))
                forwarder.makeRequest(closest.getIpAddress(), closest.getTcpServerPort(), new LookupRequest(askingNode, key, transfer, file));
            else
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getTcpServerPort(), new LookupReplyRequest(properties, key, transfer, file));
        }
    }

    /**
     * Save a file into the "/online" folder
     *
     * @param file   File that has to be saved
     * @param folder
     */
    public void saveFile(File file, String folder) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream("./node" + properties.getNodeId() + "/" + folder + "/" + file.getName());
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

    /**
     * Send the files to be assigned to other nodes
     */
    public void distributeResource(File file, boolean backup) {
        if (backup) {
            saveFile(file, "backup");
        } else {
            saveFile(file, "online");
            forwarder.makeRequest(successor().getIpAddress(), successor().getTcpServerPort(), new DistributeResourceRequest(file, true));
        }
    }

    /**
     * Sends the backup resources of the current node to its successor.
     *
     * @param properties is the node to which the resources are sent
     */
    public void giveBackupResourcesToSuccessor(NodeProperties properties) {
        File folder = new File("./node" + this.properties.getNodeId() + "/online");
        File[] allFiles = folder.listFiles();

        // TODO: after the switch to the Visitor pattern, send them as a list
        for (File file : allFiles) {
            forwarder.makeRequest(properties.getIpAddress(), properties.getTcpServerPort(), new DistributeResourceRequest(file, true));
        }
    }

    /**
     * Deletes a file in the backup folder.
     *
     * @param f is the file to be deleted
     */
    public void deleteBackupFile(File f) {
        File folder = new File("./node" + this.properties.getNodeId() + "/backup");
        File[] allFiles = folder.listFiles();

        // TODO: after the switch to the Visitor pattern, send them as a list
        for (File file : allFiles) {
            if (f.getName().equals(file.getName()))
                file.delete();
        }
    }

    /**
     * Deletes the backup folder and recreates a new one.
     */
    public void deleteBackupFolderAndRecreate() {
        File folder = new File("./node" + this.properties.getNodeId() + "/backup");
        File[] allFiles = folder.listFiles();
        for (File file : allFiles) {
            file.delete();
        }
        folder.delete();

        File f = new File("./node" + properties.getNodeId() + "/backup/backupFolderCreation");
        if (!f.getParentFile().exists())
            f.getParentFile().mkdirs();
    }

    /*
    public void transferOnLeave() {
        File folder = new File("./node" + properties.getNodeId() + "/online");
        File[] allFiles = folder.listFiles();

        for (File file : allFiles) {
            forwarder.makeRequest(successor().getIpAddress(), successor().getTcpServerPort(), new TransferAfterLeaveRequest(file));
            file.delete();
        }
    }*/

    /**
     * Notify your neighbours (Successor and predecessor) that the current node is leaving the network
     */
    public void notifyNeighbours() {
        forwarder.makeRequest(predecessor.getIpAddress(), predecessor.getTcpServerPort(), new UpdateSuccessorRequest(successor()));
        // forwarder.makeRequest(successor().getIpAddress(), successor().getTcpServerPort(), new UpdatePredecessorRequest(predecessor));
    }

    /**
     * Terminate all the threads and close the socket connection of the server side
     */
    public void close() {
        fixFingersThread.shutdownNow();
        stabilizeThread.shutdownNow();
        forwarderThread.shutdownNow();
        forwarder.stop();
        nodeSocketServer.close();
        forwarder.stop();

        if (pingSuccessor != null)
            pingSuccessor.terminate();

        if (pingSuccessorServer != null)
            pingSuccessorServer.terminate();
    }

    /**
     * Prints the coordinates of the server
     */
    public void printServerCoordinates() {
        System.out.println("Server coordinates:");
        System.out.println("ID: " + properties.getNodeId());
        System.out.println("Ip: " + properties.getIpAddress());
        System.out.println("Port: " + properties.getTcpServerPort());
        System.out.println("Coordinates: " + properties.getIpAddress() + ":" + properties.getTcpServerPort());

        System.out.println("------------------------------------------\n");
    }

    /**
     * Prints the finger table
     */
    public void printFingerTable() {
        int limit = (int) Math.pow(2, KEY_SIZE);
        int bound;
        System.out.println("Finger table node id " + properties.getNodeId() + ":");
        System.out.println("i\tvalue\tbound");

        for (int i = 0; i < KEY_SIZE; i++) {
            if (fingers[i] != null) {
                bound = (int) (Math.pow(2, i) + properties.getNodeId()) % limit;
                System.out.println("[" + i + "]\t" + fingers[i].getNodeId() + "\t\t" + bound);
            } else {
                System.out.println("-");
            }
        }

        System.out.println("------------------------------------------\n");
    }

    /**
     * Print successor and predecessor of the node
     */
    public void printPredecessorAndSuccessor() {

        System.out.println("Current node ID: " + properties.getNodeId());
        System.out.println();

        if (predecessor != null) {
            System.out.println("Predecessor coordinates:");
            System.out.println("ID: " + predecessor.getNodeId());
            System.out.println("Ip: " + predecessor.getIpAddress());
            System.out.println("Port: " + predecessor.getTcpServerPort());
            System.out.println();
        }

        if (successor() != null) {
            System.out.println("Successor coordinates:");
            System.out.println("ID: " + successor().getNodeId());
            System.out.println("Ip: " + successor().getIpAddress());
            System.out.println("Port: " + successor().getTcpServerPort());
        }

        System.out.println("------------------------------------------\n");
    }

    /**
     * Prints the list of successors
     */
    public void printSuccessors() {
        System.out.println("List of successors contained by node " + properties.getNodeId() + ":");

        for (NodeProperties n : successors) {
            System.out.println(n.getNodeId());
        }

        System.out.println("------------------------------------------\n");
    }
}

