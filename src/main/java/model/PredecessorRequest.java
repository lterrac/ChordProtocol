package model;

public class PredecessorRequest implements Request{

    private NodeProperties properties;

    public PredecessorRequest(NodeProperties properties) {
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
