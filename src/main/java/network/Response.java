package network;

import java.io.Serializable;

public interface Response extends Serializable {
    void handleResponse(ResponseHandler handler);
}