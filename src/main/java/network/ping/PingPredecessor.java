package network.ping;

import model.Node;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PingPredecessor extends PingClient {

    public PingPredecessor(Node node, String ip, int port, int targetNodeId) {
        super(node, ip, port, targetNodeId);
    }

    @Override
    void crashHandling() {
        node.setPredecessor(null);
    }

    @Override
    protected void printAMiss(Logger logger) {
        logger.log(Level.SEVERE, "\t\tMissing predecessor packet from node " + targetNodeId);
    }
}
