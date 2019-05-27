package network;

import model.Node;
import network.requests.RequestWithAck;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static model.NodeProperties.PING_PERIOD;

/**
 * Wrapper for the socket and its streams
 */
class ClientSocket {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private final AtomicBoolean stop;
    private Deque<RequestWithAck> messageQueue;


    /**
     * Future that check if the {@link #ackListenerThread} has finished
     */
    private Future<?> ackListenerDone;

    /**
     * The thread that create and starts a new timer
     */
    private ExecutorService ackListenerThread;

    ClientSocket(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        ackListenerThread = Executors.newSingleThreadExecutor();
        stop = new AtomicBoolean(false);
        messageQueue = new ArrayDeque<>();


    }

    ObjectOutputStream getOut() {
        return out;
    }

    Socket getSocket() {
        return socket;
    }

    ObjectInputStream getIn() {
        return in;
    }

    /**
     * Check if exists a thread listening for an Ack
     *
     * @return true if exists
     */
    boolean isAckListenerDone() {
        if (ackListenerDone != null) return ackListenerDone.isDone();
        else return true;
    }

    /**
     * Start the ackListener Thread and creates a new timer
     * @param node
     */
    void listenForAck(Node node) {
        ackListenerDone = ackListenerThread.submit(() -> {
            try {
                Thread.sleep(PING_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!stop.get()) {
                System.out.println("node " + socket.getInetAddress() + ":" + socket.getLocalPort() + " not reachable");
                messageQueue.forEach(requestWithAck -> requestWithAck.getAck().recovery(node, requestWithAck));
                messageQueue.clear();
            }
        });
    }

    /**
     * Enqueue the request sent to the current socket. In case of crash every request can call recovery()
     *
     * @param request
     */
    void enqueueRequest(RequestWithAck request) {
        messageQueue.addLast(request);
    }

    /**
     * When Ack is received stop the timer
     */
    void ackReceived() {
        stop.set(true);
    }

    void close() {
        try {
            ackListenerThread.shutdown();
            stop.set(true);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}