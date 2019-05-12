package network.requests;

import model.NodeProperties;
import network.RequestHandler;

public class UpdateSuccessorRequest implements Request {

    private NodeProperties properties;

    public UpdateSuccessorRequest(NodeProperties properties) {

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