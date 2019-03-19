package model;

import java.util.TimerTask;

public class CheckPredecessorTimer extends TimerTask {

    private Node node;

    public CheckPredecessorTimer(Node node) {
        this.node = node;
    }

    /**
     * The run() method is executed only if the timer expires
     */
    @Override
    public void run() {
        node.setPredecessor(null);
    }
}
