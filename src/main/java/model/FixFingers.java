package model;

/**
 * Refresh fingers table
 */
public class FixFingers implements Runnable {

    private Node node;

    public FixFingers(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        int fixIndex = node.nextFinger();
        int fixId = node.getProperties().getNodeId() + (int)Math.pow(2, fixIndex - 1);
        node.fixFingerSuccessor(node.getProperties(), fixId, fixIndex);
    }

}
