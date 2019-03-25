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

        System.out.println("Executing Fix Fingers Thread");

        int fixIndex = node.nextFinger();
        int fixId = node.getProperties().getNodeId() + (int) Math.pow(2, fixIndex);


        node.fixFingerSuccessor(node.getProperties(), fixId, fixIndex);

    }

}
