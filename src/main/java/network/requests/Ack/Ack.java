package network.requests.Ack;


import model.Node;
import network.RequestHandler;
import network.requests.Request;

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

    /**
     * recovers the request in case that the target node is unreachable
     *
     * @param node
     * @param request
     */
    public abstract void recovery(Node node, Request request);
}
