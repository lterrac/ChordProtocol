package network.requests;

import model.NodeProperties;
import network.RequestHandler;

import java.io.File;

public class DistributeResourceRequest implements Request {

    private boolean backup;
    private File file;
    private NodeProperties properties;

    public DistributeResourceRequest(NodeProperties properties, File file, boolean backup) {
        this.file = file;
        this.properties=properties;
        this.backup = backup;
    }

    public boolean isBackup() {
        return backup;
    }

    public File getFile() {
        return file;
    }

    public NodeProperties getProperties() {
        return properties;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
