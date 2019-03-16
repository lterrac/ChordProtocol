package model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import static utilities.Utilities.BUFFER_SIZE;
import static utilities.Utilities.sha1;

public class Forwarder {
    private static final Logger logger = Logger.getLogger(Forwarder.class.getName());

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
            */

            in.close();
            out.close();
            clientSocket.close();
            return response;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "An error occurred!";
    }
}
