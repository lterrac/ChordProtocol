package network.requests;

import model.NodeProperties;
import network.RequestHandler;

public class CheckPredecessorRequest implements Request {

    private NodeProperties properties;

    public CheckPredecessorRequest(NodeProperties properties) {
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
