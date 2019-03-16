package model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static utilities.Utilities.BUFFER_SIZE;

public class RequestHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(RequestHandler.class.getName());

    private Node node;

    public RequestHandler(Node node) {
        this.node = node;
    }

    public void run() {

        while (true) {
            try {
                // Accept a request from a client
                Socket client = node.getServerSocket().accept();
                InputStream in = client.getInputStream();
                OutputStream out = client.getOutputStream();

                byte[] buffer = new byte[BUFFER_SIZE];
                in.read(buffer);
                String request = new String(buffer);
                request = request.trim();

                String[] data = request.split(":");

                switch(data[0]){
                    case "ping":{
                        String response = "I'm alive!";
                        out.write(response.getBytes());
                        out.close();
                    } break;
                    case "find_successor":
                    case "successor":
                    case "predecessor":
                    case "update":
                    default: logger.log(Level.WARNING, "This request doesn't exist");
                }

                in.close();
                out.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}