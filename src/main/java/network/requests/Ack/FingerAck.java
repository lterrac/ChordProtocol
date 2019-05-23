package network.requests.Ack;

import model.Node;
import network.requests.Request;

import java.util.Deque;

public class FingerAck extends Ack {

    private final boolean isSuccessor;
    private final int fingerIndex;

    public FingerAck(boolean isSuccessor, int fingerIndex, String senderIp, int senderPort) {
        super(senderIp, senderPort);
        this.isSuccessor = isSuccessor;
        this.fingerIndex = fingerIndex;
    }

    @Override
    public void recovery(Node node, Deque<Request> requests) {
        node.retryAndUpdate(requests, isSuccessor, fingerIndex);
    }
}
