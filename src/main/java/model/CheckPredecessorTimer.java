package model;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Timer task that set the predecessor to null if the predecessor of the node does not respond
 */
public class CheckPredecessorTimer extends TimerTask {

    private final AtomicBoolean stop;
    private Node node;

    CheckPredecessorTimer(Node node) {
        this.stop = new AtomicBoolean(false);
        this.node = node;
    }

    @Override
    public void run() {
        if (!stop.get())
            node.setPredecessor(null);
    }

    void stop() {
        stop.set(true);
    }
}
