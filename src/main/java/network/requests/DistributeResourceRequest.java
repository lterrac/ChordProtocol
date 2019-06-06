package network.requests;

import network.RequestHandler;

import java.io.File;

public class DistributeResourceRequest implements Request {

    private boolean backup;
    private boolean check;
    private File file;

    public DistributeResourceRequest(File file, boolean backup, boolean check) {
        this.file = file;
        this.backup = backup;
        this.check=check;
    }

    public boolean isCheck() {
        return check;
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
