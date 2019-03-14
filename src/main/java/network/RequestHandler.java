package network;

public interface RequestHandler {
    Response handle(FindSuccessorRequest request);
}
