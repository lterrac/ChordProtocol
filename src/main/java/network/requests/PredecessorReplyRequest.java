package network.requests;

import model.NodeProperties;
import network.RequestHandler;

public class PredecessorReplyRequest implements Request {

    private NodeProperties properties;

    public PredecessorReplyRequest(NodeProperties properties) {
        this.properties = properties;
    }

    public NodeProperties getProperties() {
        return properties;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
