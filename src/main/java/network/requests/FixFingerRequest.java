package network.requests;

import model.NodeProperties;
import network.RequestHandler;
import network.requests.Ack.Ack;

public class FixFingerRequest extends RequestWithAck {

    private NodeProperties properties;
    private int fixId;
    private int fixIndex;

    public FixFingerRequest(NodeProperties properties, int fixId, int fixIndex, Ack ack) {
        super(ack);
        this.properties = properties;
        this.fixId = fixId;
        this.fixIndex = fixIndex;
    }

    public NodeProperties getProperties() {
        return properties;
    }

    public int getFixId() {
        return fixId;
    }

    public int getFixIndex() {
        return fixIndex;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.ack(getAck());
        handler.handle(this);
    }
}
