package network.requests;

import model.NodeProperties;
import network.RequestHandler;

import java.io.File;

public class LookupRequest implements Request {
    private NodeProperties properties;
    private int key;
    private boolean transfer;
    private File file;

    public LookupRequest(NodeProperties properties, int key, boolean transfer, File file) {
        this.transfer=transfer;
        this.properties = properties;
        this.key = key;
        this.file=file;
    }

    public NodeProperties getProperties() {
        return properties;
    }

    public int getKey() {
        return key;
    }
    public boolean getTransfer() {
        return transfer;
    }

    public File getFile() {
        return file;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
