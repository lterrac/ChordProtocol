package model;

import java.io.File;

public class FindSuccessorRequest implements Request{

    private NodeProperties properties;


    public FindSuccessorRequest(NodeProperties properties) {
        this.properties = properties;
    }

    public NodeProperties getProperties() {
        return properties;
    }


    @Override
    public void handleRequest(RequestHandler handler) {
        handler.handle(this);
    }

}
