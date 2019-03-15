package main.java.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HandleRequest implements Runnable {

    private Node node;

    public HandleRequest(Node node) {
        this.node = node;
    }

    public void run() {

        while (true) {
            try {
                Socket client = null;
                ObjectInputStream in = null;
                ObjectOutputStream out = null;

                client = node.getServerSocket().accept();
                out = new ObjectOutputStream(client.getOutputStream());
                in = new ObjectInputStream(client.getInputStream());

                // TODO: get and handle the request

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}