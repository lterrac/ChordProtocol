package model;

import java.util.Timer;

public class Stabilize implements Runnable {

    private final Node node;
    private NodeProperties successorPredecessor;
    private Timer timer;
    private NodeProperties currentNode;
    private NodeProperties successor;


    public Stabilize(Node node) {
        this.node = node;
    }

    @Override
    public void run() {

        currentNode = node.getProperties();
        successor = node.successor();

        if (!successor.equals(currentNode)) {
            //Ask to the successor for its predecessor
            node.forward(currentNode, successor.getIpAddress(), successor.getPort(), "predecessor", 0, 0, 0,null);

        } else {
            if (node.isPredecessorSet()) {
                this.successorPredecessor = node.getPredecessor();
                node.setSuccessor(successorPredecessor);
                // System.out.println("Successor set in stabilize thread is: " + node.successor().getNodeId());
                //node.forward(currentNode, node.successor().getIpAddress(), node.successor().getPort(), "notify", 0, 0, 0,null);

            }
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Inform the new successor that the current node might be its predecessor
        node.forward(currentNode, node.successor().getIpAddress(), node.successor().getPort(), "notify", 0, 0, 0,null);

    }

    public void finalizeStabilize(NodeProperties successorPredecessor) {

        this.successorPredecessor = successorPredecessor;

        // System.out.println("new successorPredecessor is" + successorPredecessor.getNodeId());

        //If the predecessor of the successor is not the current node, set the new successor of the current node
        if (successorPredecessor.isInIntervalStrict(currentNode.getNodeId(), successor.getNodeId())) {
            node.setSuccessor(successorPredecessor);

            // System.out.println("Successor set in stabilize thread is: " + node.successor().getNodeId());
            //node.forward(currentNode, node.successor().getIpAddress(), node.successor().getPort(), "notify", 0, 0, 0,null);

        }
    }

    public void cancelTimer() {
        timer.cancel();
    }
}
/*

package model;


public class Stabilize implements Runnable {

    private final Node node;
            }
        }
    }

    public void finalizeStabilize(NodeProperties successorPredecessor) {
        if (successorPredecessor != null) {
            this.successorPredecessor = successorPredecessor;


            //If the predecessor of the successor is not the current node, set the new successor of the current node
            if (successorPredecessor.isInIntervalStrict(currentNode.getNodeId(), successor.getNodeId())) {
                node.setSuccessor(successorPredecessor);
            }
        }

        //Inform the new successor that the current node might be its predecessor
        node.forward(currentNode, node.successor().getIpAddress(), node.successor().getPort(), "notify", 0, 0, 0, null);
    }
}*/
