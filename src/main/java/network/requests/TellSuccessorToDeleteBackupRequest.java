package network.requests;

import network.RequestHandler;

import java.io.File;

public class TellSuccessorToDeleteBackupRequest implements Request {
    private final File file;

    public TellSuccessorToDeleteBackupRequest(File f) {
        file = f;
    }

    public File getFile() {
        return file;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
