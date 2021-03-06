package model;

import network.Forwarder;
import network.NodeSocketServer;
import network.requests.Ack.FingerAck;
import network.requests.Ack.PredecessorAck;
import network.requests.Ack.UnknownAck;
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
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static model.NodeProperties.*;
import static utilities.Utilities.calculateFixId;
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
     * Represents the "client side" of a node. It sends requests to other nodes
     */
    private Forwarder forwarder;

    /**
     * Contains information about the node
     */
    private NodeProperties properties;

    private String senderIp;
    private int senderPort;


    /**
     * List of adjacent successors of the node
     */
    private Deque<NodeProperties> successors;

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

    /**
     * Socket server for accepting new socket connections
     */
    private NodeSocketServer nodeSocketServer;
    private ServerSocket serverSocket;

    /**
     * Useful to save the index of the finger table to which to apply the fix_finger algorithm
     */
    private int n_fix;

    public Node() {
        n_fix = -1;
        successors = new ArrayDeque<>(KEY_SIZE);

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

    public void setPredecessor(NodeProperties predecessor) {
        this.predecessor = predecessor;
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
            System.out.println(e.getMessage());
        }

        assert currentIp != null;
        return currentIp.getHostAddress();
    }

    /**
     * Get the finger 0 of the finger table
     *
     * @return Node successor
     */
    NodeProperties successor() {
        return fingers[0];
    }

    /**
     * Set the successor of the current node
     *
     * @param node the successor
     */
    public void setSuccessor(NodeProperties node) {
        synchronized (fingers) {
            this.fingers[0] = node;
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
            System.out.println(e.getMessage());
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
        forwarder.makeRequest(ip, port, new FindSuccessorRequest(properties, new UnknownAck(senderIp, senderPort)));
        startThreads();
        nodeSocketServer = new NodeSocketServer(this);

        new Thread(nodeSocketServer).start();
    }

    //TODO Check what the fuck is wrong with this method

    /**
     * Check if the current node is the last in the network
     *
     * @return true if it is the last one, otherwise false
     */
    private boolean isNodeAlone() {
        if (predecessor.equals(properties)) {
            return properties.equals(successor());
        }
        return false;
    }

    /**
     * Pubblish the resources into the network.
     * It moves them from the folder "/offline" to "/online"
     */
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
                forwarder.makeRequest(successor().getIpAddress(), successor().getPort(), new DistributeResourceRequest(null, file,
                        new FingerAck(true, -1, false, null, senderIp, senderPort)));
                file.delete();
            }
        }
        System.out.println("You correctly published your resources! Some of them could have been forwarded to other nodes, while some could still be of your property placed in your online folder");

    }

    /**
     * Ask to the successor if it holds some resources that needs to be managed by the current node
     */
    public void askSuccessorForResources() {
        forwarder.makeRequest(successor().getIpAddress(), successor().getPort(),
                new AskSuccessorResourcesRequest(this.getProperties(), new FingerAck(true, -1, false, null, senderIp, senderPort)));
    }

    /**
     * Check if some file needs to be managed by the predecessor and in case sends the files back and deletes its copy.
     * The predecessor is passed as a parameter because, when a node joins the network, {@link #predecessor} may not
     * be already set by the {@link #notifySuccessor(NodeProperties)}
     *
     * @param nodeProperties predecessor of the node
     */
    public void giveResourcesToPredecessor(NodeProperties nodeProperties) {
        File folder = new File("./node" + this.getProperties().getNodeId() + "/online");
        File[] allFiles = folder.listFiles();
        for (File f : allFiles) {
            if (checkResourcesForPredecessor(sha1(f.getName()), nodeProperties.getNodeId(), properties.getNodeId())) {
                forwarder.makeRequest(nodeProperties.getIpAddress(), nodeProperties.getPort(),
                        new DistributeResourceRequest(nodeProperties, f, new UnknownAck(senderIp, senderPort)));
                f.delete();
            }
        }
    }

    /**
     * Creates the primary classes of the node.
     * Create : - ServerSocket to accept incoming connections
     * - Set
     */
    private void startNode() {
        serverSocket = createServerSocket();
        senderIp = getCurrentIp();
        senderPort = serverSocket.getLocalPort();
        initializeNode(senderIp, senderPort);
        forwarder = new Forwarder(this);
        foldersCreation();
    }

    /**
     * Create the folders "/online" and "/offline" for the node in which its resources will be stored
     */
    private void foldersCreation() {
        File f = new File("./node" + properties.getNodeId() + "/offline/offlineFolderCreation");
        if (!f.getParentFile().exists())
            f.getParentFile().mkdirs();
        File g = new File("./node" + this.getProperties().getNodeId() + "/online/" + "onlineFolderCreation");
        if (!g.getParentFile().exists())
            g.getParentFile().mkdirs();
    }

    /**
     * Initialize the attributes of the node:
     * - Creates the {@link NodeProperties} of the node
     * - Set the {@link #successor} to the node itself
     * - Set the {@link #predecessor} to null
     *
     * @param ipAddress IP of the node
     * @param port      Port on which the node is listening
     */
    private void initializeNode(String ipAddress, int port) {
        this.properties = new NodeProperties(sha1(ipAddress + ":" + port), ipAddress, port);
        setSuccessor(this.properties);
        this.predecessor = null;
    }

    /**
     *
     */
    public void notifyNeighbours() {
        forwarder.makeRequest(predecessor.getIpAddress(), predecessor.getPort(),
                new UpdateSuccessorRequest(successor(), new UnknownAck(senderIp, senderPort)));
        forwarder.makeRequest(successor().getIpAddress(), successor().getPort(),
                new UpdatePredecessorRequest(getPredecessor(),
                        new FingerAck(true, -1, false, null, senderIp, senderPort)));
    }

    /**
     * Starts the routines : - Check Predecessor
     * - Fix Fingers
     * - Stabilize
     * - Forwarder (check for unused sockets)
     */
    private void startThreads() {
        checkPredecessorThread = Executors.newSingleThreadScheduledExecutor();
        fixFingersThread = Executors.newSingleThreadScheduledExecutor();
        stabilizeThread = Executors.newSingleThreadScheduledExecutor();
        forwarderThread = Executors.newSingleThreadScheduledExecutor();

        forwarderThread.scheduleAtFixedRate(forwarder, 1, CHECK_SOCKET_PERIOD, TimeUnit.MILLISECONDS);
        checkPredecessorThread.scheduleAtFixedRate(checkPredecessor, 800, 4500, TimeUnit.MILLISECONDS);
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
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(),
                    new FindSuccessorReplyRequest(successor(), new UnknownAck(senderIp, senderPort)));
        } else {
            ClosestNode closest = closestPrecedingNode(askingNode.getNodeId());

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.getProperties().equals(properties))
                forwarder.makeRequest(closest.getProperties().getIpAddress(), closest.getProperties().getPort(),
                        new FindSuccessorRequest(askingNode,
                                new FingerAck(closest.isSuccessor(), closest.getFingerIndex(), closest.fromSuccessorList(), closest.getSuccessorId(), senderIp, senderPort)));
            else
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(),
                        new FindSuccessorReplyRequest(properties, new UnknownAck(senderIp, senderPort)));
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
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(),
                    new FixFingerReplyRequest(successor(), fixId, fixIndex, new UnknownAck(senderIp, senderPort)));
        } else {
            ClosestNode closest = closestPrecedingNode(fixId);

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.getProperties().equals(properties)) {
                forwarder.makeRequest(closest.getProperties().getIpAddress(), closest.getProperties().getPort(),
                        new FixFingerRequest(askingNode, fixId, fixIndex,
                                new FingerAck(closest.isSuccessor(), closest.getFingerIndex(), closest.fromSuccessorList(), closest.getSuccessorId(), senderIp, senderPort)));
            } else {
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(),
                        new FixFingerReplyRequest(properties, fixId, fixIndex,
                                new UnknownAck(senderIp, senderPort)));
            }
        }
    }

    /**
     * Find the highest predecessor of id.
     *
     * @param nodeId is the id of the target node
     * @return the closest node to the target one
     */
    private ClosestNode closestPrecedingNode(int nodeId) {
        ClosestNode closest = new ClosestNode();
        closest.setProperties(properties);
        boolean found = false;

        // first check the finger table
        for (int i = KEY_SIZE - 1; i >= 0 && !found; i--) {
            if (fingers[i] != null && fingers[i].isInInterval(properties.getNodeId(), nodeId)) {
                closest.setProperties(fingers[i]);
                if (i != 0) {
                    closest.setFingerIndex(i);
                    closest.setIsSuccessor(false);
                } else {
                    closest.setFingerIndex(0);
                    closest.setIsSuccessor(true);
                }
                found = true;
            }
        }

        // then check the successors list
        for (NodeProperties n : successors) {
            //TODO CHECK!
            if (n.isInInterval(closest.getProperties().getNodeId(), nodeId)) {
                closest.setProperties(n);
                closest.setFingerIndex(-1);
                closest.setIsSuccessor(false);
                closest.setFromSuccessorList(true);
                closest.setSuccessorId(String.valueOf(n.getNodeId()));
            }
        }
        return closest;
    }


    /**
     * Look for the owner of a resource in the net.
     * TODO: check
     *
     * @param key is the hash of the resource you're looking for in the net
     */
    public void lookup(NodeProperties askingNode, int key) {

        if (isInIntervalInteger(properties.getNodeId(), key, successor().getNodeId())) {
            forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(),
                    new LookupReplyRequest(successor(), key,
                            new UnknownAck(senderIp, senderPort)));
        } else {
            ClosestNode closest = closestPrecedingNode(key);

            //if the closestPrecedingNode is not the same as the current Node (Happens only when there is only one node in the net
            if (!closest.getProperties().equals(properties))
                forwarder.makeRequest(closest.getProperties().getIpAddress(), closest.getProperties().getPort(),
                        new LookupRequest(askingNode, key,
                                new FingerAck(closest.isSuccessor(), closest.getFingerIndex(), closest.fromSuccessorList(), closest.getSuccessorId(), senderIp, senderPort)));
            else
                forwarder.makeRequest(askingNode.getIpAddress(), askingNode.getPort(),
                        new LookupReplyRequest(properties, key,
                                new UnknownAck(senderIp, senderPort)));
        }
    }

    /**
     * Save a file into the "/online" folder
     *
     * @param file File that has to be saved
     */
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
     * Send a message to {@link #predecessor} to check if it is alive
     */
    void checkPredecessor() {
        forwarder.makeRequest(predecessor.getIpAddress(), predecessor.getPort(), new CheckPredecessorRequest(properties, new PredecessorAck(senderIp, senderPort)));
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
     * Send the files to be assigned to other nodes
     */
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
                forwarder.makeRequest(successor().getIpAddress(), successor().getPort(),
                        new DistributeResourceRequest(null, file,
                                new FingerAck(true, -1, false, null, senderIp, senderPort)));
                return;
            }


            //TODO ADD closestprecedingnode()

            // if the resource must be sent to one of the fingers
            for (int i = 0; i < KEY_SIZE - 1; i++) {
                int lowerBound = calculateFixId(properties.getNodeId(), i);
                int upperBound = calculateFixId(properties.getNodeId(), i + 1);

                if (i + 1 <= highestIndex && isInIntervalInteger(lowerBound, fileId, upperBound)) {
                    /*System.out.println("___________________________________________________________________________________________________________________DISTRIBUTE FOR______________");
                    System.out.println("File " + fileId + " to " + fingers[i].getNodeId());*/
                    forwarder.makeRequest(fingers[i].getIpAddress(), fingers[i].getPort(),
                            new DistributeResourceRequest(null, file,
                                    new FingerAck(false, i, false, null, senderIp, senderPort)));
                    return;
                }

            }

            // if the resource is out of the scope of the finger table forward it to the last finger, but only if it's not yourself
            if (fingers[highestIndex].getNodeId() != properties.getNodeId()) {
                /*System.out.println("___________________________________________________________________________________________________________________DISTRIBUTE LAST______________");
                System.out.println("File " + fileId + " to " + fingers[highestIndex].getNodeId());*/
                forwarder.makeRequest(fingers[highestIndex].getIpAddress(), fingers[highestIndex].getPort(),
                        new DistributeResourceRequest(null, file,
                                new FingerAck(false, highestIndex, false, null, senderIp, senderPort)));
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

    public void printServerCoordinates() {
        System.out.println("Server coordinates:");
        System.out.println("ID: " + properties.getNodeId());
        System.out.println("Ip: " + properties.getIpAddress());
        System.out.println("Port: " + properties.getPort());
        System.out.println("Coordinates: " + properties.getIpAddress() + ":" + properties.getPort());

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

    public void printSuccessors() {
        System.out.println("List of successors contained by node " + properties.getNodeId() + ":");

        for (NodeProperties n : successors) {
            System.out.println(n.getNodeId());
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


    public void transferOnLeave() {
        File folder = new File("./node" + properties.getNodeId() + "/online");
        File[] allFiles = folder.listFiles();

        // TODO: after the switch to the Visitor pattern, send them as a list
        for (File file : allFiles) {
            forwarder.makeRequest(successor().getIpAddress(), successor().getPort(),
                    new TransferAfterLeaveRequest(file,
                            new FingerAck(true, -1, false, null, senderIp, senderPort)));
            file.delete();
        }
    }

    void updateSuccessors(Deque<NodeProperties> sList) {
        successors = sList;
    }

    public Deque<NodeProperties> getCustomizedSuccessors() {
        Deque<NodeProperties> newList = new ArrayDeque<>(successors);

        if (newList.size() >= KEY_SIZE) {
            newList.removeLast();
        }
        newList.addFirst(properties);

        return newList;
    }

    public void retryAndUpdate(RequestWithAck request, boolean isSuccessor, int fingerIndex, boolean fromSuccessorList, String successorId) {
        //If a successor was chosen and there is more than one successor in the successor list
        if (fromSuccessorList) {
            NodeProperties closest = null;
            if (successors.size() > 1) {
                Iterator<NodeProperties> iterator = successors.descendingIterator();
                while (iterator.hasNext()) {
                    NodeProperties node = iterator.next();
                    if (Integer.parseInt(successorId) == node.getNodeId()) {
                        iterator.remove();
                        closest = iterator.next();
                        break;
                    }
                }
            } else
                closest = successors.getFirst();

            if (closest != null) {
                FingerAck ack = (FingerAck) request.getAck();
                ack.setSuccessorId(closest.getNodeId());
                forwarder.makeRequest(closest.getIpAddress(), closest.getPort(), request);
            }
        } else {
            if (isSuccessor) {
                // update the successor
                setSuccessor(successors.removeFirst());

                // send again
                forwarder.makeRequest(successor().getIpAddress(), successor().getPort(), request);
            } else {
                fingers[fingerIndex] = null;

                FingerAck ack = (FingerAck) request.getAck();

                if (fingerIndex > 1)
                    ack.setFingerIndex(fingerIndex - 1);
                else
                    ack.setIsSuccessor(true);

                // send again
                forwarder.makeRequest(fingers[fingerIndex - 1].getIpAddress(), fingers[fingerIndex - 1].getPort(), request);
            }
        }


    }
}

