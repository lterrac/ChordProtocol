package model;

import java.io.File;

public class DistributeResourceRequest implements Request{

    private File file;
    private NodeProperties properties;

    public DistributeResourceRequest(NodeProperties properties, File file) {
        this.file = file;
        this.properties=properties;
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
