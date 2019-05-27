package model;

public class ClosestNode {
    private NodeProperties properties;
    private int fingerIndex;
    private boolean isSuccessor;
    private boolean fromSuccessorList;
    private String successorId;

    public ClosestNode() {
        fingerIndex = -1;
        isSuccessor = false;
        fromSuccessorList = false;
        successorId = null;
        properties = null;
    }

    public void setProperties(NodeProperties properties) {
        this.properties = properties;
    }

    public void setFingerIndex(int fingerIndex) {
        this.fingerIndex = fingerIndex;
    }

    public void setIsSuccessor(boolean isSuccessor) {
        this.isSuccessor = isSuccessor;
    }

    public NodeProperties getProperties() {
        return properties;
    }

    public void setFromSuccessorList(boolean fromSuccessorList) {
        this.fromSuccessorList = fromSuccessorList;
    }

    public void setSuccessorId(String successorId) {
        this.successorId = successorId;
    }

    public boolean isSuccessor() {
        return isSuccessor;
    }

    public boolean fromSuccessorList() {
        return fromSuccessorList;
    }

    public int getFingerIndex() {
        return fingerIndex;
    }

    public String getSuccessorId() {
        return successorId;
    }
}
