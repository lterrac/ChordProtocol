package network;

public class FindSuccessorRequest implements Request{

    @Override
    public Response handleRequest(RequestHandler handler) {
        return handler.handle(this);
    }
}