package network.ping;

import model.Node;

import java.util.logging.Level;


public class PingSuccessor extends PingClient {

    public PingSuccessor(Node node, String ip, int port, int targetNodeId) {
        super(node, ip, port, targetNodeId);
    }

    @Override
    void crashHandling() {
        node.replaceSuccessor();
    }

    @Override
    protected void printAMiss() {
        PingClient.logger.log(Level.SEVERE, "Missing successor packet from node " + targetNodeId);
    }
}
