package model;

import java.io.Serializable;

public interface Request extends Serializable {
    void handleRequest(RequestHandler handler);

}
