package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class Forwarder {
    private static final Logger logger = Logger.getLogger(Forwarder.class.getName());

    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public synchronized void makeRequest(NodeProperties nodeInformation, String ip, int port, String message, int fixId, int fixIndex, int lookupKey) {

        try {
            clientSocket = new Socket(ip, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            Message msg = new Message(nodeInformation, message, fixId, fixIndex, lookupKey);
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

/*
public String makeRequest(String ip, int port, String message) {
        Socket clientSocket = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            clientSocket = new Socket(ip, port);
            out = clientSocket.getOutputStream();
            in = clientSocket.getInputStream();

            // Send the request
            out.write(message.getBytes());
            byte[] buffer = new byte[BUFFER_SIZE];

            // Get the response
            in.read(buffer);
            String response = new String(buffer);
            response = response.trim();

            System.out.println("Request to node " + sha1(ip + ":" + port));
            System.out.println("Response is: " + response);

            /* TODO: gestire l'eventuale trasferimento di file
            if(msg.contains("transfer_files")){
                String[] name = p.split(":");
                System.out.println("**** Receiving File *****");
                System.out.println("File count "+name.length);
                for(int i=0;i<name.length;i++)
                {
                    System.out.println("File received "+name[i]);
                    File f = new File("./nodeFile/" +name[i]);
                    f.createNewFile();
                }
            }


            in.close();
                    out.close();
                    clientSocket.close();
                    return response;

                    } catch (IOException e) {
                    e.printStackTrace();
                    }
                    return "An error occurred!";
                    }

 */
