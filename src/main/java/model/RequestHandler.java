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

            String request = ((String) in.readObject());

            switch (request) {
                case "ping": {
                    String response = "I'm alive!";
                    out.write(response.getBytes());
                    out.close();
                }
                break;
                case "find_successor":
                case "successor":
                case "predecessor":
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
