package network.requests.Ack;

import model.Node;
import network.requests.DistributeResourceRequest;
import network.requests.Request;


public class UnknownAck extends Ack {

    public UnknownAck(String senderIp, int senderPort) {
        super(senderIp, senderPort);
    }

    @Override
    public void recovery(Node node, Request request) {
        //TODO Write better
        if (request instanceof DistributeResourceRequest)
            node.saveFile(((DistributeResourceRequest) request).getFile());

    }


}
