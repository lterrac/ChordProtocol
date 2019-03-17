package model;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RequestHandler implements Runnable {

    private Socket client;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private boolean stop;
    private Node node;
    private static final Logger logger = Logger.getLogger(RequestHandler.class.getName());

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
            //TODO Decide to use Visitor pattern or not.
              In this case the object will call the handle() method which will be
              managed by the method overridden in the Node class. Otherwise keep the switch
              case and call different methods, always defined in node.

              example : RequestObj request = ((RequestObj) in.readObject()).handle(node);
            */

            Message msg = ((Message) in.readObject());

            switch (msg.getMessage()) {
                case "ping": {
                    String response = "I'm alive!";
                    out.write(response.getBytes());
                    out.close();
                }
                break;
                case "find_successor": {
                    //search for the successor of the node received from the network
                    node.findSuccessor(msg.getProperties());
                }
                break;
                case "found_successor": {
                    //Set the successor of the current node to the one received from the network
                    node.successor(msg.getProperties());
                }
                case "successor":
                case "predecessor": {
                    //Send the predecessor of the current node to the one that asked for it
                    String receiverIp = msg.getProperties().getIpAddress();
                    int receiverPort = msg.getProperties().getPort();
                    node.getForwarder().makeRequest(node.getProperties(), receiverIp, receiverPort, "sent_predecessor");
                }
                break;
                case "sent_predecessor": {
                    //Once the predecessor is arrived, set it into the dedicated thread and call notify()
                    //todo Check if a synchronized block is necessary
                    node.getStabilize().setSuccessorPredecessor(msg.getProperties());
                    node.getStabilize().notify();
                }
                break;
                case "notify" : {
                    node.notifySuccessor(msg.getProperties());
                }
                break;
                case "update":
                default:
                    logger.log(Level.WARNING, "This request doesn't exist");
            }
        } catch (EOFException | SocketException e) {
            if (!stop) {
                System.out.print("EOF: ");
                close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        stop = true;
    }

    /**
     * Method that closes ClientHandler connection
     */
    public void close() {
        System.out.println("Closing down connection");
        stop();
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        if (out != null) {
            try {
                out.close();
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
}
