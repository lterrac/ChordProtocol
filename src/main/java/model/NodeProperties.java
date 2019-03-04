package model;

public class NodeProperties {

    private int nodeId;
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
}
