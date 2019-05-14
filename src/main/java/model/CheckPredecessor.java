package model;

import java.util.Timer;

import static model.NodeProperties.CHECK_PERIOD;

/**
 * Check if predecessor has failed
 */
public class CheckPredecessor implements Runnable {

    private Node node;
    private CheckPredecessorTimer task;

    CheckPredecessor(Node node) {
        this.node = node;
    }

    /**
     * Ping the predecessor to discover if it is still alive
     */
    @Override
    public void run() {
        if (node.isPredecessorSet()) {
            // Create the task and set the timer
            task = new CheckPredecessorTimer(node);
            (new Timer()).schedule(task, CHECK_PERIOD);

            node.checkPredecessor();
        }
    }

    void cancelTimer() {
        if (task != null)
            task.stop();
    }
}
