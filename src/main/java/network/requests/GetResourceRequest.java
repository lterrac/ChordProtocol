package network.requests;

import model.NodeProperties;
import network.RequestHandler;

public class GetResourceRequest implements Request {
    private int fileId;
    private NodeProperties askingNode;

    public GetResourceRequest(NodeProperties asking, int fileId) {
        this.fileId = fileId;
        this.askingNode = asking;
    }

    public int getFileId() {
        return fileId;
    }

    public NodeProperties getAskingNode() {
        return askingNode;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
