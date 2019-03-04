package model;

import java.io.Serializable;
import java.util.HashMap;

public class Node {


    private NodeProperties nodeProperties;
    private HashMap<Integer, Serializable> data = new HashMap<>();

    private NodeProperties successor;
    private NodeProperties predecessor;
    private NodeProperties[] fingers;



    /**
     * Create a new Chord Ring
     */
    public void create() {

    }

    /**
     * Join a Ring containing the know Node
     * @param knownNode used to join the ring
     */
    public void join(Node knownNode) {

    }

    //TODO: Will go into a thread
    /**
     *  Veriﬁes n’s immediate successor, and tells the successor about n.
     */
    public void stabilize() {

    }

    /**
     *
     * @param predecessor node that could be the predecessor
     */
    public void notifySuccessor(Node predecessor) {

    }

    //TODO: Will go into a thread
    /**
     * Refresh fingers table
     */
    public void fixFingers() {

    }

    //TODO: Will go into a thread
    /**
     * Check if predecessor has failed
     */
    public void checkPredecessor() {

    }

    /**
     * Find the successor of the node with the given id
     * @param nodeId
     */
    public void findSuccessor(int nodeId) {

    }

    /**
     * Find the highest predecessor of id
     * @param nodeId
     */
    public void closestPrecedingNode(int nodeId) {

    }


}
