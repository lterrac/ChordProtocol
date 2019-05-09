package model;

import java.util.Timer;

import static model.NodeProperties.CHECK_PERIOD;

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
        if (node.isPredecessorSet()) {
            node.checkPredecessor();

            // Set the timer
            timer = new Timer();
            CheckPredecessorTimer task = new CheckPredecessorTimer(node);
            timer.schedule(task, CHECK_PERIOD);
        }
    }

    public void cancelTimer() {
        if(timer != null) {
            timer.cancel();
        }
    }
}
