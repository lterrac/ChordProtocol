package network.requests;

import model.NodeProperties;
import network.RequestHandler;

import java.io.File;

public class LookupRequest implements Request {
    private File file;
    private boolean transfer;
    private NodeProperties properties;
    private int key;

    public LookupRequest(NodeProperties properties, int key, boolean transfer, File file) {
        this.properties = properties;
        this.key = key;
        this.transfer = transfer;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public boolean isTransfer() {
        return transfer;
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
