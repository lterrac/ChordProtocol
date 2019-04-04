package model;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class Forwarder {
    private static final Logger logger = Logger.getLogger(Forwarder.class.getName());

    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public synchronized void makeRequest(NodeProperties nodeInformation, String ip, int port, String message, int fixId, int fixIndex, int lookupKey, File[] allFiles) {

        try {
            clientSocket = new Socket(ip, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            Message msg = new Message(nodeInformation, message, fixId, fixIndex, lookupKey, allFiles);
            out.writeObject(msg);
            out.flush();

            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the active connection
     */
    private void close() {
        try {
            out.close();
            in.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
