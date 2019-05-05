package model;

public class UpdatePredecessorRequest implements Request {

    private NodeProperties properties;

    public UpdatePredecessorRequest(NodeProperties properties) {

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
