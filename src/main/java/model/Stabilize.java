package model;

import network.requests.Ack.FingerAck;
import network.requests.NotifyRequest;
import network.requests.PredecessorRequest;

import java.util.Deque;

public class Stabilize implements Runnable {

    private final Node node;
    private NodeProperties successorPredecessor;
    private NodeProperties currentNode;
    private NodeProperties successor;


    Stabilize(Node node) {
        this.node = node;
    }

    @Override
    public void run() {

        currentNode = node.getProperties();
        successor = node.successor();

        if (!successor.equals(currentNode)) {
            //Ask to the successor for its predecessor

            String successorIp = node.successor().getIpAddress();
            int successorPort = node.successor().getPort();
            String senderIp = node.getProperties().getIpAddress();
            int senderPort = node.getProperties().getPort();

            node.getForwarder().makeRequest(successorIp, successorPort,
                    new PredecessorRequest(currentNode,
                            new FingerAck(true, -1, false, null, senderIp, senderPort)));

        } else {
            if (node.isPredecessorSet()) {
                this.successorPredecessor = node.getPredecessor();
                node.setSuccessor(successorPredecessor);
            }
        }
    }

    void finalizeStabilize(NodeProperties successorPredecessor, Deque<NodeProperties> successors) {

        //Update successors list with the new one received by our successor
        node.updateSuccessors(successors);

        if (successorPredecessor != null) {
            this.successorPredecessor = successorPredecessor;

            //If the predecessor of the successor is not the current node, set the new successor of the current node
            if (successorPredecessor.isInIntervalStrict(currentNode.getNodeId(), successor.getNodeId())) {
                node.setSuccessor(successorPredecessor);
            }
        }

        String successorIp = node.successor().getIpAddress();
        int successorPort = node.successor().getPort();
        String senderIp = node.getProperties().getIpAddress();
        int senderPort = node.getProperties().getPort();

        //Inform the new successor that the current node might be its predecessor
        node.getForwarder().makeRequest(successorIp, successorPort, new NotifyRequest(currentNode, new FingerAck(true, -1, false, null, senderIp, senderPort)));
    }
}