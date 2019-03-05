package model;

public class NodeProperties {

    //TODO It would be better having nodeId as a number (but 160 bit)
    private final String nodeId;
    public static final int KEY_SIZE = 160;
    private String ipAddress;
    private int port;

    public NodeProperties(String nodeId, String ipAddress, int port) {
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
