package network.requests;

import model.NodeProperties;
import network.RequestHandler;
import network.requests.Ack.Ack;

public class FindSuccessorReplyRequest extends RequestWithAck {

    private NodeProperties properties;

    public FindSuccessorReplyRequest(NodeProperties properties, Ack ack) {
        super(ack);
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
