package model;

import java.util.Timer;

/**
 * Check if predecessor has failed
 */
public class CheckPredecessor implements Runnable {

    private Node node;
    private Timer timer;

    public CheckPredecessor(Node node) {
        this.node = node;
    }

    /**
     * Ping the predecessor to discover if it is still alive
     */
    @Override
    public void run() {

        node.checkPredecessor();

        // Set the timer
        timer = new Timer();
        CheckPredecessorTimer task = new CheckPredecessorTimer(node);
        timer.schedule(task, NodeProperties.CHECK_TIME);
    }

    public void cancelTimer() {
        timer.cancel();
    }
}
