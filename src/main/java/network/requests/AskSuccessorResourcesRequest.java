package network.requests;

import model.NodeProperties;
import network.RequestHandler;

public class AskSuccessorResourcesRequest implements Request {

    private NodeProperties nodeProperties;

    public AskSuccessorResourcesRequest(NodeProperties nodeProperties) {
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
