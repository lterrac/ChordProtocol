package network;

import model.AckTimer;
import model.Node;
import network.requests.RequestWithAck;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static model.NodeProperties.PING_PERIOD;

/**
 * Wrapper for the socket and its streams
 */
public class ClientSocket {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    /**
     * Future that check if the {@link #ackListenerThread} has finished
     */
    private Future<?> ackListenerDone;

    /**
     * The thread that create and starts a new timer
     */
    private ExecutorService ackListenerThread;

    /**
     * The timer responsible for the recovery of the messages
     */
    private AckTimer ackTimer;

    public ClientSocket(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        ackListenerThread = Executors.newSingleThreadExecutor();
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectInputStream getIn() {
        return in;
    }

    /**
     * Check if exists a thread listening for an Ack
     *
     * @return true if exists
     */
    public boolean isAckListenerDone() {
        if (ackListenerDone != null) {
            System.out.println("Future not null " + ackListenerDone.isDone());
            ackListenerDone.isDone();
        }

        System.out.println("Future null");
        return true;
    }

    /**
     * Start the ackListener Thread and creates a new timer
     * @param node
     */
    public void listenForAck(Node node) {
        ackTimer = new AckTimer(node);
        ackListenerDone = ackListenerThread.submit(() -> new Timer().schedule(ackTimer, PING_PERIOD));
    }

    /**
     * Enqueue the request sent to the current socket. In case of crash every request can call recovery()
     *
     * @param request
     */
    public void enqueueRequest(RequestWithAck request) {
        ackTimer.enqueue(request);
    }

    /**
     * When Ack is received stop the timer
     */
    public void ackReceived() {
        ackTimer.stop();
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
