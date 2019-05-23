package network.requests;

import model.NodeProperties;
import network.RequestHandler;
import network.requests.Ack.Ack;

public class LookupRequest extends RequestWithAck {

    private NodeProperties properties;
    private int key;

    public LookupRequest(NodeProperties properties, int key, Ack ack) {
        super(ack);
        this.properties = properties;
        this.key = key;
    }

    public NodeProperties getProperties() {
        return properties;
    }

    public int getKey() {
        return key;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
