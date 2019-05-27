package network.requests;

import model.NodeProperties;
import network.RequestHandler;
import network.requests.Ack.Ack;

import java.io.File;

public class DistributeResourceRequest extends RequestWithAck {

    private File file;
    private NodeProperties properties;

    public DistributeResourceRequest(NodeProperties properties, File file, Ack ack) {
        super(ack);
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
        handler.ack(getAck());
        handler.handle(this);
    }
}
