package network.requests;

import model.NodeProperties;
import network.RequestHandler;
import network.requests.Ack.Ack;

public class UpdatePredecessorRequest extends RequestWithAck {

    private NodeProperties properties;

    public UpdatePredecessorRequest(NodeProperties properties, Ack ack) {
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
