package model;

import java.io.File;

public class TransferAfterLeaveRequest implements Request{

    private File file;

    public TransferAfterLeaveRequest(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
