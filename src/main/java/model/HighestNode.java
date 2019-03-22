package model;

public class HighestNode implements Runnable {
    private final Node node;
    private boolean highestNode;

    public HighestNode(Node node, boolean highestNode) {
        this.highestNode = highestNode;
        this.node = node;
    }

    @Override
    public void run() {
        if (node.getFingers()[0].getNodeId() < node.getProperties().getNodeId()) {
            highestNode = true;
        }
    }

    public boolean isHighestNode() {
        return highestNode;
    }

    public void setToFalse() {
        highestNode = false;
    }
}
