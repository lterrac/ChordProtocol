package main.java.model;

public class NodeProperties {

    public static final int KEY_SIZE = 4;

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
}
