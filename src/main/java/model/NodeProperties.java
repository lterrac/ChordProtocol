package model;

import java.io.Serializable;

/**
 * Wraps the properties of the node
 */
public class NodeProperties implements Serializable {

    public static final int KEY_SIZE = 8; // size of the keys
    static final int CHECK_PERIOD = 200; // waiting time for the checkPredecessor request
    static final int FIX_PERIOD = 200; // fixFingers period
    static final int CHECK_SOCKET_PERIOD = 10000; // check for unused sockets period
    static final int STABILIZE_PERIOD = 200; // stabilize period


    private final int nodeId;
    private String ipAddress;
    private int port;

    public NodeProperties(int nodeId, String ipAddress, int port) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.port = port;
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

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
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
            boolean port = getPort() == node.getPort();
            boolean ip = getIpAddress().equals(node.getIpAddress());
            return id && port && ip;
        } else return false;
    }
}
