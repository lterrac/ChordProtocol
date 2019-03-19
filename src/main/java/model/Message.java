package model;

import java.io.Serializable;

public class Message implements Serializable {

    private NodeProperties properties;
    private String message;

    public Message(NodeProperties properties, String message) {
        this.message = message;
        this.properties = properties;
    }

    public NodeProperties getProperties() {
        return properties;
    }
    public String getMessage() {
        return message;
    }
}
