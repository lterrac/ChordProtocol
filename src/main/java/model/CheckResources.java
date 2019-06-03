package model;

public class CheckResources implements Runnable {
    private Node node;

    public CheckResources(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        if(node.isPredecessorSet()){
            node.checkResources();
        }
    }
}
