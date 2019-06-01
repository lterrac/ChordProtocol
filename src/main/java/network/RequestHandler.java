package network;

import model.Node;
import network.requests.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;


public class RequestHandler extends Thread implements RequestHandlerInterface {

    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private Socket client;
    private boolean stop;
    private Node node;

    public RequestHandler(Socket client, Node node) throws IOException {
        this.client = client;
        this.out = new ObjectOutputStream(client.getOutputStream());
        this.in = new ObjectInputStream(client.getInputStream());
        this.node = node;
        stop = false;
    }

    @Override
    public void run() {
        try {
            do {
                readResponse();
            } while (!stop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readResponse() {
        try {
            ((Request) in.readObject()).handleRequest(this);
        } catch (EOFException | SocketException e) {
            if (!stop) {
                close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void stopping() {
        stop = true;
    }

    /**
     * Method that closes ClientHandler connection
     */
    private void close() {
        stopping();
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        try {
            client.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour giving backup resources to the successor
     *
     * @param request is the handled request
     */
    @Override
    public void handle(AskPredecessorBackupResourcesRequest request) {
        node.giveBackupResourcesToSuccessor(request.getProperties());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour deleting the backup file contained in the request
     *
     * @param request is the handled request
     */
    @Override
    public void handle(TellSuccessorToDeleteBackupRequest request) {
        node.deleteBackupFile(request.getFile());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour
     *
     * @param request is the handled request
     */
    @Override
    public void handle(FindSuccessorRequest request) {
        node.findSuccessor(request.getProperties());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour distributing the file contained in the request
     *
     * @param request is the handled request
     */
    @Override
    public void handle(DistributeResourceRequest request) {
        node.distributeResource(request.getFile(), request.isBackup());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour asking the resources to the successor
     *
     * @param request is the handled request
     */
    @Override
    public void handle(AskSuccessorResourcesRequest request) {
        node.giveResourcesToPredecessor(request.getProperties());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour setting the predecessor
     *
     * @param request is the handled request
     */
    @Override
    public void handle(UpdatePredecessorRequest request) {
        node.setPredecessor(request.getProperties());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour updating the successor and asking it the resources
     *
     * @param request is the handled request
     */
    @Override
    public void handle(UpdateSuccessorRequest request) {
        node.setSuccessor(request.getProperties());
        node.askSuccessorForResources();
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour setting the successor and asking it the resources
     *
     * @param request is the handled request
     */
    @Override
    public void handle(FindSuccessorReplyRequest request) {
        node.setSuccessor(request.getProperties());
        node.askSuccessorForResources();
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour
     *
     * @param request is the handled request
     */
    @Override
    public void handle(FixFingerRequest request) {
        node.fixFingerSuccessor(request.getProperties(), request.getFixId(), request.getFixIndex());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour updating the i-th finger
     *
     * @param request is the handled request
     */

    @Override
    public void handle(FixFingerReplyRequest request) {
        node.updateFinger(request.getFixIndex(), request.getProperties());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour doing the lookup
     *
     * @param request is the handled request
     */
    @Override
    public void handle(LookupRequest request) {
        node.lookup(request.getProperties(), request.getKey(), request.isTransfer(), request.getFile());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour printing the needed information in case of simple lookup or sending the resource in case of lookup associated to a distribute resource
     *
     * @param request is the handled request
     */
    @Override
    public void handle(LookupReplyRequest request) {
        if (!request.isTransfer())
            System.out.println("The resource " + request.getKey() + " is contained by node " + request.getProperties().getNodeId());
        else
            node.getForwarder().makeRequest(request.getProperties().getIpAddress(), request.getProperties().getTcpServerPort(), new DistributeResourceRequest(request.getFile(), false));
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour saving the file in the online folder and sending it to the successor as backup resource
     *
     * @param request is the handled request
     */
    @Override
    public void handle(TransferAfterLeaveRequest request) {
        node.saveFile(request.getFile(), "online");
        node.getForwarder().makeRequest(node.successor().getIpAddress(), node.successor().getTcpServerPort(), new DistributeResourceRequest(request.getFile(), true));
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour sending the predecessor of the current node to the one that asked for it
     *
     * @param request is the handled request
     */

    @Override
    public void handle(PredecessorRequest request) {

        String receiverIp = request.getProperties().getIpAddress();
        int receiverPort = request.getProperties().getTcpServerPort();

        //TODO Check if it is the right behaviour
        node.getForwarder().makeRequest(receiverIp, receiverPort, new PredecessorReplyRequest(node.getPredecessor(), node.getCustomizedSuccessors()));
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour doing this: once the predecessor is arrived, set it into the dedicated thread and call notify()
     *
     * @param request is the handled request
     */

    //todo Check if a synchronized block is necessary
    @Override
    public void handle(PredecessorReplyRequest request) {
        node.finalizeStabilize(request.getProperties(), request.getSuccessors());
    }

    /**
     * Through this method the Request Handler associated to the current node manages the incoming request behaviour
     *
     * @param request is the handled request
     */
    @Override
    public void handle(NotifyRequest request) {
        node.notifySuccessor(request.getProperties());
    }

}
