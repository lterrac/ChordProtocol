package model;

import java.util.Timer;

/**
 * Veriﬁes n’s immediate successor, and tells the successor about n.
 */
public class Stabilize implements Runnable {

    private final Node node;
    private NodeProperties successorPredecessor;
    private Timer timer;
    private NodeProperties currentNode;
    private NodeProperties successor;


    public Stabilize(Node node) {
        this.node = node;
    }

    @Override
    public void run() {

        currentNode = node.getProperties();
        successor = node.successor();

        // System.out.println("Current Node in Stabilize is:" + currentNode.getNodeId());
        // System.out.println("Successor Node in Stabilize is:" + successor.getNodeId());

        if (!successor.equals(currentNode)) {
            //Ask to the successor for its predecessor
            node.forward(currentNode, successor.getIpAddress(), successor.getPort(), "predecessor", 0, 0, 0);
/*
            //Wait for the response coming from the successor
            timer = new Timer();
            StabilizeTimer task = new StabilizeTimer(currentNode, successor, successorPredecessor, node);
            timer.schedule(task, NodeProperties.CHECK_PERIOD);
*/
        } else {
            if (node.isPredecessorSet()) {
                this.successorPredecessor = node.getPredecessor();
                node.setSuccessor(successorPredecessor);
                // System.out.println("Successor set in stabilize thread is: " + node.successor().getNodeId());
            }
        }

        //Inform the new successor that the current node might be its predecessor
        node.forward(currentNode, successor.getIpAddress(), successor.getPort(), "notify", 0, 0, 0);

    }

    public void setSuccessorPredecessor(NodeProperties successorPredecessor) {
        this.successorPredecessor = successorPredecessor;

        // System.out.println("                                                new successorPredecessor is" + successorPredecessor.getNodeId());

        //If the predecessor of the successor is not the current node, set the new successor of the current node
        if (successorPredecessor.isInIntervalStrict(currentNode.getNodeId(), successor.getNodeId())) {
            node.setSuccessor(successorPredecessor);
            // System.out.println("Successor set in stabilize thread is: " + node.successor().getNodeId());
        }
    }

    public void cancelTimer() {
        timer.cancel();
    }
}
