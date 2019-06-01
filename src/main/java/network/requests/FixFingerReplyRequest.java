package network.requests;

import model.NodeProperties;
import network.RequestHandler;

public class FixFingerReplyRequest implements Request {

    private NodeProperties properties;
    private int fixId;
    private int fixIndex;

    public FixFingerReplyRequest(NodeProperties properties, int fixId, int fixIndex) {
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
        handler.handle(this);
    }
}
