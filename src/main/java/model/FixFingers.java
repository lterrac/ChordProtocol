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
        int fixId = calculateFixId(fixIndex);

        node.fixFingerSuccessor(node.getProperties(), fixId, fixIndex);
    }

    /**
     * Calculate the fixId to check. Keep attention that if id+2^(fixIndex)
     * is greater than the maximum key value ( 2^(KEY_SIZE) - 1 ) it must return the diffenrence
     * between the two values.
     *
     * @param fixIndex
     * @return
     */
    private int calculateFixId(double fixIndex) {
        int ideal = node.getProperties().getNodeId() + (int) Math.pow(2, fixIndex);
        int limit = (int) Math.pow(2, NodeProperties.KEY_SIZE) - 1;
        if (ideal > limit)
            return ideal - limit - 1;
        else return ideal;
    }

}
