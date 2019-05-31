package model;

import java.io.Serializable;

/**
 * Wraps the properties of the node
 */
public class NodeProperties implements Serializable {

    public static final int KEY_SIZE = 8; // size of the keys
    static final int CHECK_PREDECESSOR_TIMEOUT = 8000; // waiting time for the checkPredecessor request
    static final long CHECK_PREDECESSOR_SCHEDULE = 9000;
    static final int FIX_PERIOD = 200; // fixFingers period
    static final int CHECK_SOCKET_PERIOD = 10000; // check for unused sockets period
    static final int STABILIZE_PERIOD = 500; // stabilize period


    private final int nodeId;
    private String ipAddress;
    private int tcpServerPort;
    private int udpSuccessorServerPort;
    private int udpPredecessorServerPort;

    public NodeProperties(int nodeId, String ipAddress, int tcpServerPort, int udpSuccessorServerPort, int udpPredecessorServerPort) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.tcpServerPort = tcpServerPort;
        this.udpSuccessorServerPort = udpSuccessorServerPort;
        this.udpPredecessorServerPort = udpPredecessorServerPort;
    }

    /**
     * Check if the value is in the interval between (firstBound, secondBound]
     *
     * @param firstBound  the first bound
     * @param value       the value to check
     * @param secondBound the second bound
     * @return true if the value is in the interval
     */
    static boolean isInIntervalInteger(int firstBound, int value, int secondBound) {
        if (firstBound < secondBound)
            return value > firstBound && value <= secondBound;
        else
            return value > firstBound || value <= secondBound;
    }

    static boolean checkResourcesForPredecessor(int value, int pred, int curr) {
        if (pred < curr)
            return value > curr || value <= pred;
        else
            return value > curr && value <= pred;

    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getTcpServerPort() {
        return tcpServerPort;
    }

    public int getUdpSuccessorServerPort() {
        return udpSuccessorServerPort;
    }

    public int getUdpPredecessorServerPort() {
        return udpPredecessorServerPort;
    }

    public int getNodeId() {
        return nodeId;
    }

    /**
     * Check if the node id is in the interval between (firstBound, secondBound]
     *
     * @param firstBound  the first bound
     * @param secondBound the second bound
     * @return true if the node id is in the interval
     */
    boolean isInInterval(int firstBound, int secondBound) {
        if (firstBound < secondBound)
            return nodeId > firstBound && nodeId <= secondBound;
        else
            return nodeId > firstBound || nodeId <= secondBound;
    }

    /**
     * Check if the node id is in the interval between (firstBound, secondBound)
     *
     * @param firstBound  the first bound
     * @param secondBound the second bound
     * @return true if the node id is in the interval
     */
    boolean isInIntervalStrict(int firstBound, int secondBound) {
        if (firstBound < secondBound)
            return nodeId > firstBound && nodeId < secondBound;
        else
            return nodeId > firstBound || nodeId < secondBound;
    }

    /**
     * Check if the two NodeProperties have the same ID
     *
     * @param obj Node to compare with this
     * @return true if the objects have the same node id
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NodeProperties) {
            NodeProperties node = (NodeProperties) obj;
            boolean id = nodeId == node.getNodeId();
            boolean port = getTcpServerPort() == node.getTcpServerPort();
            boolean ip = getIpAddress().equals(node.getIpAddress());
            return id && port && ip;
        } else return false;
    }
}
