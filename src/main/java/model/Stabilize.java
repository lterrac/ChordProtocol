package model;

import network.requests.NotifyRequest;
import network.requests.PredecessorRequest;

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
            node.getForwarder().makeRequest( successor.getIpAddress(), successor.getPort(), new PredecessorRequest(currentNode));

        } else {
            if (node.isPredecessorSet()) {
                this.successorPredecessor = node.getPredecessor();
                node.setSuccessor(successorPredecessor);
            }
        }
    }

    void finalizeStabilize(NodeProperties successorPredecessor) {
        if (successorPredecessor != null) {
            this.successorPredecessor = successorPredecessor;

            //If the predecessor of the successor is not the current node, set the new successor of the current node
            if (successorPredecessor.isInIntervalStrict(currentNode.getNodeId(), successor.getNodeId())) {
                node.setSuccessor(successorPredecessor);
            }
        }

        //Inform the new successor that the current node might be its predecessor
        node.getForwarder().makeRequest( node.successor().getIpAddress(), node.successor().getPort(), new NotifyRequest(currentNode));
    }
}