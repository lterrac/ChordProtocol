package network.requests;

import network.RequestHandler;
import network.requests.Ack.Ack;

import java.io.File;

public class TransferAfterLeaveRequest extends RequestWithAck {

    private File file;

    public TransferAfterLeaveRequest(File file, Ack ack) {
        super(ack);
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.ack(getAck());
        handler.handle(this);
    }
}
