package network;

import model.*;
import network.requests.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;


public class RequestHandler extends Thread implements RequestHandlerInterface {

    private static final Logger logger = Logger.getLogger(RequestHandler.class.getName());
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
            /*
              In this case the object will call the handle() method which will be
              managed by the method overridden in the Node class. Otherwise keep the switch
              case and call different methods, always defined in node.

              example : RequestObj request = ((RequestObj) in.readObject()).handle(node);
            */

            //Read request
            Request request = (Request) in.readObject();

            //send ACK
            ack();

            //Handle request
            request.handleRequest(this);

        } catch (EOFException | SocketException e) {
            if (!stop) {
                close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void ack() throws IOException {
        out.writeObject(new Ack());
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


    @Override
    public void handle(Ack request) {
       //TODO Scrivi qualcosa
    }

    //search for the successor of the node received from the network
    @Override
    public void handle(FindSuccessorRequest request) {
        node.findSuccessor(request.getProperties());
    }

    @Override
    public void handle(DistributeResourceRequest request) {
        node.distributeResource(request.getProperties(),request.getFile());
    }

    @Override
    public void handle(AskSuccessorResourcesRequest request) {
        node.giveResourcesToPredecessor(request.getProperties());
    }

    @Override
    public void handle(UpdatePredecessorRequest request) {
        node.setPredecessor(request.getProperties());
    }

    @Override
    public void handle(UpdateSuccessorRequest request) {
        node.setSuccessor(request.getProperties());
        node.askSuccessorForResources();
    }

    @Override
    public void handle(FindSuccessorReplyRequest request) {
        node.setSuccessor(request.getProperties());
        node.askSuccessorForResources();
    }

    @Override
    public void handle(FixFingerRequest request) {
        node.fixFingerSuccessor(request.getProperties(), request.getFixId(), request.getFixIndex());
    }

    // update the i-th finger
    @Override
    public void handle(FixFingerReplyRequest request) {
        node.updateFinger(request.getFixIndex(), request.getProperties());
    }

    @Override
    public void handle(LookupRequest request) {
        node.lookup(request.getProperties(), request.getKey());
    }

    @Override
    public void handle(LookupReplyRequest request) {
        System.out.println("The resource " + request.getKey() + " is contained by node " + request.getProperties().getNodeId());
    }

    @Override
    public void handle(CheckPredecessorRequest request) {
        String senderIp = request.getProperties().getIpAddress();
        int senderPort = request.getProperties().getPort();
        node.getForwarder().makeRequest( senderIp, senderPort, new CheckPredecessorReplyRequest(node.getProperties()));
    }

    @Override
    public void handle(CheckPredecessorReplyRequest request) {
        node.cancelCheckPredecessorTimer();
    }

    @Override
    public void handle(TransferAfterLeaveRequest request) {
        node.saveFile(request.getFile());
    }

    //Send the predecessor of the current node to the one that asked for it
    @Override
    public void handle(PredecessorRequest request) {

        String receiverIp = request.getProperties().getIpAddress();
        int receiverPort = request.getProperties().getPort();

        //TODO Check if it is the right behaviour
        node.getForwarder().makeRequest( receiverIp, receiverPort, new PredecessorReplyRequest(node.getPredecessor(), node.getCustomizedSuccessors()));
    }

    //Once the predecessor is arrived, set it into the dedicated thread and call notify()
    //todo Check if a synchronized block is necessary
    @Override
    public void handle(PredecessorReplyRequest request) { node.finalizeStabilize(request.getProperties(), request.getSuccessors()); }

    @Override
    public void handle(NotifyRequest request) {
        node.notifySuccessor(request.getProperties());
    }

}
