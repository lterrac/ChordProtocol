package network.requests;

import network.requests.Ack.Ack;

public abstract class RequestWithAck implements Request {

    private final Ack ack;

    public RequestWithAck(Ack ack) {
        this.ack = ack;
    }

    public Ack getAck() {
        return ack;
    }
}
