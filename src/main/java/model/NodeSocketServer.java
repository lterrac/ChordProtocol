package model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class NodeSocketServer implements Runnable {

    private static final Logger logger = Logger.getLogger(NodeSocketServer.class.getName());
    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private Node node;
    private boolean stop;

    public NodeSocketServer(Node node) {
        stop = false;
        this.node = node;
        pool = Executors.newCachedThreadPool();
        serverSocket = node.getServerSocket();
    }

    public void run() {

        while (!stop) {
            // Accept a request from a client
            Socket client;

            try {
                client = node.getServerSocket().accept();
                pool.submit(new RequestHandler(client, node));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        pool.shutdown();
    }

    public void close() {
        stop = true;
    }
}