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
        int next = node.nextFinger();
        int index = node.getProperties().getNodeId() + (int)Math.pow(2, next - 1);
        node.fixFingerSuccessor(node.getProperties(), index);
    }

}
