package model;

public class LookupRequestReply implements Request{

    private NodeProperties properties;
    private int key;

    public LookupRequestReply(NodeProperties properties, int key) {
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
        handler.handle(this );
    }
}
