package model;

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

        System.out.println("Executing Stabilize Thread");

        NodeProperties currentNode = node.getProperties();
        NodeProperties successor = node.getFingers()[0];

        System.out.println("Current Node in Stabilize is:" +currentNode.getNodeId());
        System.out.println("Successor Node in Stabilize is:" + successor.getNodeId());

        //Ask to the successor for its predecessor
        node.forward(currentNode, successor.getIpAddress(), successor.getPort(), "predecessor", 0, 0, 0);

        //Wait for the response coming from the successor
        synchronized (this) {

            //todo decide if a 6 seconds timeout is enough!
            try {
                wait(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

        }

        //If the predecessor of the successor is not the current node, set the new successor of the current node
        if (successorPredecessor != null && successorPredecessor.isInIntervalStrict(currentNode.getNodeId(), successor.getNodeId())) {
            node.setSuccessor(successorPredecessor);
            System.out.println("Successor set in stabilize thread is: " + node.getFingers()[0].getNodeId());
        }


        //Inform the new successor that the current node might be its predecessor
        node.forward(currentNode, successor.getIpAddress(), successor.getPort(), "notify", 0, 0, 0);

    }

    public void setSuccessorPredecessor(NodeProperties successorPredecessor) {
        this.successorPredecessor = successorPredecessor;
    }
}
