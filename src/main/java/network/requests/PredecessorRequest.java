package network.requests;


import model.NodeProperties;
import network.RequestHandler;
import network.requests.Ack.Ack;

public class PredecessorRequest extends RequestWithAck {

    private NodeProperties properties;

    public PredecessorRequest(NodeProperties properties, Ack ack) {
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
