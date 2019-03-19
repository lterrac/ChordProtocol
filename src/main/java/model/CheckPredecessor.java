package model;

import java.util.Timer;

/**
 * Check if predecessor has failed
 */
public class CheckPredecessor implements Runnable {

    private Node node;
    private Timer timer;

    public CheckPredecessor(Node node){
        this.node = node;
    }

    public Timer getTimer() {
        return timer;
    }

    /**
     * Ping the predecessor to discover if it is still alive
     */
    @Override
    public void run() {
        NodeProperties predecessor = node.getPredecessor();

        // Set the timer
        timer = new Timer();
        CheckPredecessorTimer task = new CheckPredecessorTimer(node);
        timer.schedule(task, NodeProperties.CHECK_TIME);

        node.forward(node.getProperties(), predecessor.getIpAddress(), predecessor.getPort(), "check_predecessor");
    }

    public void cancelTimer(){
        if(timer != null ){
            timer.cancel();
        }
    }
}
