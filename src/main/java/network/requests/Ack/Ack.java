package network.requests.Ack;


import model.Node;
import network.RequestHandler;
import network.requests.Request;

import java.util.Deque;

public abstract class Ack {
    private final String senderIp;
    private final int senderPort;

    public Ack(String senderIp, int senderPort) {
        this.senderIp = senderIp;
        this.senderPort = senderPort;
    }

    public String getIpAndPort() {
        return senderIp + ":" + senderPort;
    }

    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }

    public abstract void recovery(Node node, Deque<Request> requests);
}
