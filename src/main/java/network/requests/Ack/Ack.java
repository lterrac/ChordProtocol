package network.requests.Ack;


import model.Node;
import network.RequestHandler;
import network.requests.Request;
import network.requests.RequestWithAck;

import java.io.Serializable;

public abstract class Ack implements Serializable, Request {
    private final String senderIp;
    private final int senderPort;

    public Ack(String senderIp, int senderPort) {
        this.senderIp = senderIp;
        this.senderPort = senderPort;
    }

    public String getIpAndPort() {
        return senderIp + ":" + senderPort;
    }
    /**
     * recovers the request in case that the target node is unreachable
     *  @param node
     * @param request
     */
    public abstract void recovery(Node node, RequestWithAck request);

    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }
}
