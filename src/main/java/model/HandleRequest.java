package main.java.model;

import main.java.model.Node;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class HandleRequest implements Runnable {

    private Node node;

    public HandleRequest(Node node) {
        this.node=node;
    }

    public void run() {
        ServerSocket server = null;
        int port = node.getProperties().getPort();

        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            node.setFreePort(false);
            System.out.println("The port "+port+" is already bound");
            //e.printStackTrace();
        }

        while(true){

        }

    }

}