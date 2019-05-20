package network.requests;

import model.NodeProperties;
import network.RequestHandler;

public class Ack implements Request {
    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
