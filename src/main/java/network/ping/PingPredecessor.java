package network.ping;

import model.Node;

public class PingPredecessor extends PingClient {

    public PingPredecessor(Node node, String ip, int port) {
        super(node, ip, port);
    }

    @Override
    void crashHandling() {
        node.setPredecessor(null);
    }
}
