package network.requests;

import network.RequestHandler;

import java.io.File;

public class GetResourceReply implements Request{
    private File f;

    public GetResourceReply(File f) {
        this.f = f;
    }

    public File getF() {
        return f;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
