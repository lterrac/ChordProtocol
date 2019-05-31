package network.requests;

import network.RequestHandler;

import java.io.File;

public class DistributeResourceRequest implements Request {

    private boolean backup;
    private File file;

    public DistributeResourceRequest(File file, boolean backup) {
        this.file = file;
        this.backup = backup;
    }

    public boolean isBackup() {
        return backup;
    }

    public File getFile() {
        return file;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
