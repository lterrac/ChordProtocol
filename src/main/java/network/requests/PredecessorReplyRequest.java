package network.requests;

import model.NodeProperties;
import network.RequestHandler;

import java.util.Deque;

public class PredecessorReplyRequest implements Request {

    private NodeProperties properties;
    private Deque<NodeProperties> successors;

    public PredecessorReplyRequest(NodeProperties properties, Deque<NodeProperties> successors) {
        this.properties = properties;
        this.successors = successors;
    }

    public NodeProperties getProperties() {
        return properties;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }

    public Deque<NodeProperties> getSuccessors() {
        return successors;
    }
}
