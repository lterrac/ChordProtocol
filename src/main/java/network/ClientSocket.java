package network;

import model.AckTimer;
import model.Node;
import network.requests.Ack.Ack;
import network.requests.Request;

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
public class ClientSocket implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Future<?> ackListenerDone;
    private ExecutorService ackListenerThread;
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

    public boolean isAckListenerDone() {
        if (ackListenerDone != null)
            ackListenerDone.isDone();
        return false;
    }

    public void listenForAck(Node node, Ack ack) {
        ackTimer = new AckTimer(node);
        ackTimer.addAckType(ack);
        ackListenerDone = ackListenerThread.submit(this);
    }

    @Override
    public void run() {
        new Timer().schedule(ackTimer, PING_PERIOD);
    }

    public void enqueueRequest(Request request) {
        ackTimer.enqueue(request);
    }

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
