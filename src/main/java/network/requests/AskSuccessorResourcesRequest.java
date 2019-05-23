package network.requests;

import model.NodeProperties;
import network.RequestHandler;
import network.requests.Ack.Ack;

public class AskSuccessorResourcesRequest extends RequestWithAck {

    private NodeProperties nodeProperties;

    public AskSuccessorResourcesRequest(NodeProperties nodeProperties, Ack ack) {
        super(ack);
        this.nodeProperties = nodeProperties;
    }

    public NodeProperties getProperties() {
        return nodeProperties;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
