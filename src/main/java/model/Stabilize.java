package model;

import sun.awt.AWTAccessor;

/**
 * Veriﬁes n’s immediate successor, and tells the successor about n.
 */
public class Stabilize implements Runnable {

    private final Node node;
    private NodeProperties successorPredecessor;


    public Stabilize(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        NodeProperties currentNode = node.getProperties();
        NodeProperties successor = node.getFingers()[0];

        //Ask to the successor for its predecessor
        node.forward(currentNode, successor.getIpAddress(), successor.getPort(), "predecessor", 0, 0, 0);

        //Wait for the response coming from the successor
        synchronized (this) {

            //todo decide if a timeout is necessary!
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

        }

        //If the predecessor of the successor is not the current node, set the new successor of the current node
        System.out.println("CI ARRIVA");
        if (successorPredecessor.isInIntervalStrict(currentNode.getNodeId(), successor.getNodeId())) {
            node.setSuccessor(successorPredecessor);
        }

        //Inform the new successor that the current node might be its predecessor
        node.forward(currentNode, successor.getIpAddress(), successor.getPort(), "notify", 0, 0, 0);

    }

    public void setSuccessorPredecessor(NodeProperties successorPredecessor) {
        this.successorPredecessor = successorPredecessor;
    }
}
