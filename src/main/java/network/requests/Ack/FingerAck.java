package network.requests.Ack;

import model.Node;
import network.requests.RequestWithAck;

public class FingerAck extends Ack {

    private boolean isSuccessor;
    private int fingerIndex;
    private boolean fromSuccessorList;
    private String successorId;

    public FingerAck(boolean isSuccessor, int fingerIndex, boolean fromSuccessorList, String successorId, String senderIp, int senderPort) {
        super(senderIp, senderPort);
        this.isSuccessor = isSuccessor;
        this.fingerIndex = fingerIndex;
        this.successorId = successorId;
        this.fromSuccessorList = fromSuccessorList;
    }

    /**
     * If the node try to contact a finger and it does not respond, try to send it to another node
     *  @param node
     * @param requests
     */
    @Override
    public void recovery(Node node, RequestWithAck requests) {
        node.retryAndUpdate(requests, isSuccessor, fingerIndex, fromSuccessorList, successorId);
    }

    public void setSuccessorId(int successorId) {
        this.successorId = String.valueOf(successorId);
    }

    public void setFingerIndex(int index) {
        fingerIndex = index;
    }

    public void setIsSuccessor(boolean isSuccessor) {
        this.isSuccessor = isSuccessor;
    }
}
