package network.requests.Ack;

import model.Node;
import network.requests.Request;

public class FingerAck extends Ack {

    private final boolean isSuccessor;
    private final int fingerIndex;

    public FingerAck(boolean isSuccessor, int fingerIndex, String senderIp, int senderPort) {
        super(senderIp, senderPort);
        this.isSuccessor = isSuccessor;
        this.fingerIndex = fingerIndex;
    }

    /**
     * If the node try to contact a finger and it does not respond, try to send it to another node
     *
     * @param node
     * @param requests
     */
    @Override
    public void recovery(Node node, Request requests) {
        node.retryAndUpdate(requests, isSuccessor, fingerIndex);
    }
}
