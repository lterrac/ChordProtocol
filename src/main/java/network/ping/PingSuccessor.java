package network.ping;

import model.Node;

public class PingSuccessor extends PingClient {

    public PingSuccessor(Node node, String ip, int port) {
        super(node, ip, port);
    }

    @Override
    void crashHandling() {
        node.replaceSuccessor();
    }
}
