package network.ping;

import model.Node;

import java.util.logging.Level;

public class PingPredecessor extends PingClient {

    public PingPredecessor(Node node, String ip, int port, int targetNodeId) {
        super(node, ip, port, targetNodeId);
    }

    @Override
    void crashHandling() {
        node.setPredecessor(null);
    }

    @Override
    protected void printAMiss() {
        PingClient.logger.log(Level.SEVERE, "\t\tMissing predecessor packet from node " + targetNodeId);
    }
}
