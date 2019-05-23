package network;

import model.Node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    /**
     * Listen for incoming connections and creates a {@link network.RequestHandler} for every one of them
     */
    public void run() {

        while (!stop) {
            // Accept a request from a client
            Socket client;

            try {
                client = node.getServerSocket().accept();
                ;
                pool.submit(new RequestHandler(new ClientSocket(client, new ObjectInputStream(client.getInputStream()),
                        new ObjectOutputStream(client.getOutputStream())), node));
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

    /**
     * Stop listening on the port
     */
    public void close() {
        stop = true;
    }
}