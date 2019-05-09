package network.requests;

import model.NodeProperties;
import network.RequestHandler;

public class FindSuccessorRequest implements Request {

    private NodeProperties properties;


    public FindSuccessorRequest(NodeProperties properties) {
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
