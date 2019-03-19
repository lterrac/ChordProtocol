package model;

import java.io.Serializable;

public class Message implements Serializable {

    private NodeProperties properties;
    private String message;
    private int fixIndex;

    /**
     * Constructor of Message objects
     *
     * @param properties is the set of properties of a node
     * @param message is the string that specifies the kind of request
     * @param fixIndex is the index of the finger to be updated
     */
    public Message(NodeProperties properties, String message, int fixIndex) {
        this.message = message;
        this.properties = properties;
        this.fixIndex = fixIndex;
    }

    public NodeProperties getProperties() {
        return properties;
    }
    public String getMessage() {
        return message;
    }

    public int getFixIndex(){
        return fixIndex;
    }
}
