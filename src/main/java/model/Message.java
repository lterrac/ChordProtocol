package model;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {

    private NodeProperties properties;
    private String message;
    private int fixId;
    private int fixIndex;
    private int key;
    private File[]allFiles;

    /**
     * Constructor of Message objects
     *
     * @param properties is the set of properties of a node
     * @param message    is the string that specifies the kind of request
     * @param fixId      is the upper bound Id of the fixIndex-th row of the finger table
     * @param fixIndex   is the index of the finger to be updated
     * @param key        is the hash of the key to be searched on the net
     */
    public Message(NodeProperties properties, String message, int fixId, int fixIndex, int key, File[] allFiles) {
        this.message = message;
        this.properties = properties;
        this.fixId = fixId;
        this.fixIndex = fixIndex;
        this.key = key;
        this.allFiles=allFiles;
    }

    public NodeProperties getProperties() {
        return properties;
    }

    public String getMessage() {
        return message;
    }

    public int getFixId() {
        return fixId;
    }

    public int getFixIndex() {
        return fixIndex;
    }

    public int getKey() {
        return key;
    }

    public File[] getAllFiles() {
        return allFiles;
    }


}
