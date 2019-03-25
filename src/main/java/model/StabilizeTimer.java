package model;

import java.util.TimerTask;

public class StabilizeTimer extends TimerTask {

    private final Node node;
    private final NodeProperties successorPredecessor;
    private final NodeProperties currentNode;
    private final NodeProperties successor;

    public StabilizeTimer(NodeProperties currentNode, NodeProperties successor, NodeProperties successorPredecessor, Node node) {
        this.currentNode = currentNode;
        this.successor = successor;
        this.successorPredecessor = successorPredecessor;
        this.node = node;
    }

    @Override
    public void run() {
        //If the predecessor of the successor is not the current node, set the new successor of the current node
        if (successorPredecessor != null && successorPredecessor.isInIntervalStrict(currentNode.getNodeId(), successor.getNodeId())) {
            node.setSuccessor(successorPredecessor);
            System.out.println("Successor set in stabilize thread is: " + node.successor().getNodeId());
        }
    }
}
