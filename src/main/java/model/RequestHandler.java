package model;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RequestHandler implements Runnable {

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
            //TODO Decide to use Visitor pattern or not.
              In this case the object will call the handle() method which will be
              managed by the method overridden in the Node class. Otherwise keep the switch
              case and call different methods, always defined in node.

              example : RequestObj request = ((RequestObj) in.readObject()).handle(node);
            */

            Message msg = ((Message) in.readObject());

            switch (msg.getMessage()) {
                case "ping": {
                    String senderIp = msg.getProperties().getIpAddress();
                    int senderPort = msg.getProperties().getPort();
                    node.forward(msg.getProperties(), senderIp, senderPort, "ping_reply", 0);
                }
                break;
                case "ping_reply": {
                    System.out.println("Ping message received from node " + msg.getProperties().getNodeId());
                }
                break;
                case "check_predecessor": {
                    String senderIp = msg.getProperties().getIpAddress();
                    int senderPort = msg.getProperties().getPort();
                    node.forward(node.getProperties(), senderIp, senderPort, "check_predecessor_reply", 0);
                }
                break;
                case "check_predecessor_reply": {
                    node.cancelCheckPredecessorTimer();
                }
                break;
                case "fix_finger": {
                    node.fixFingerSuccessor(msg.getProperties(), msg.getFixIndex());
                }
                break;
                case "fix_finger_reply": {
                    // update the i-th finger
                    node.updateFinger(msg.getFixIndex(), msg.getProperties());
                }
                break;
                case "find_successor": {
                    //search for the successor of the node received from the network
                    node.findSuccessor(msg.getProperties());
                }
                break;
                case "found_successor": {
                    //Set the successor of the current node to the one received from the network
                    node.setSuccessor(msg.getProperties());
                }
                case "successor":
                case "predecessor": {
                    //Send the predecessor of the current node to the one that asked for it
                    String receiverIp = msg.getProperties().getIpAddress();
                    int receiverPort = msg.getProperties().getPort();
                    node.forward(node.getProperties(), receiverIp, receiverPort, "sent_predecessor", 0);
                }
                break;
                case "sent_predecessor": {
                    //Once the predecessor is arrived, set it into the dedicated thread and call notify()
                    //todo Check if a synchronized block is necessary
                    node.setSuccessorPredecessor(msg.getProperties());
                }
                break;
                case "notify": {
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
    private void close() {
        System.out.println("Closing down connection");
        stop();
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
}
