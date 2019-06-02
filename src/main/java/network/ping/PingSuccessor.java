package network.ping;

import model.Node;

import java.util.logging.Level;
import java.util.logging.Logger;


public class PingSuccessor extends PingClient {

    public PingSuccessor(Node node, String ip, int port, int targetNodeId) {
        super(node, ip, port, targetNodeId);
    }

    @Override
    void crashHandling() {
        node.replaceSuccessor();
    }
    @Override
    protected void printAMiss(Logger logger) {
        logger.log(Level.SEVERE, "Missing successor packet from node " + targetNodeId);
    }
}
