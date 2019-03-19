package model;

import java.io.Serializable;

public class NodeProperties implements Serializable {

    public static final int KEY_SIZE = 4; // size of the keys
    public static final int CHECK_TIME = 6000; // waiting time for the checkPredecessor request

    private final int nodeId;
    private String ipAddress;
    private int port;

    public NodeProperties(int nodeId, String ipAddress, int port) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.port = port;
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

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isInInterval(int min, int max) {
        return nodeId > min && nodeId <= max;
    }

    public boolean isInIntervalStrict(int min, int max) { return nodeId > min && nodeId < max; }
}
