package network.requests.Ack;

import model.Node;
import network.requests.Request;

import java.util.Deque;

public class PredecessorAck extends Ack {

    public PredecessorAck(String senderIp, int senderPort) {
        super(senderIp, senderPort);
    }

    @Override
    public void recovery(Node node, Deque<Request> requests) {
        node.setPredecessor(null);
    }
}
