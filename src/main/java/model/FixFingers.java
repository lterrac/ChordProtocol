package model;

import static utilities.Utilities.calculateFixId;

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
        int fixId = calculateFixId(node.getProperties().getNodeId(), fixIndex);

        node.fixFingerSuccessor(node.getProperties(), fixId, fixIndex);
    }
}
