package network.requests;

import model.NodeProperties;
import network.RequestHandler;
import network.requests.Ack.Ack;

import java.util.Deque;

public class PredecessorReplyRequest extends RequestWithAck {

    private NodeProperties properties;
    private Deque<NodeProperties> successors;

    public PredecessorReplyRequest(NodeProperties properties, Deque<NodeProperties> successors, Ack ack) {
        super(ack);
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
