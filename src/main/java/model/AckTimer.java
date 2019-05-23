package model;

import network.requests.Ack.Ack;
import network.requests.Request;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class AckTimer extends TimerTask {

    private final AtomicBoolean stop;
    private Node node;
    private Deque<Request> messageQueue;
    private Ack ackType;

    public AckTimer(Node node) {
        this.stop = new AtomicBoolean(false);
        this.node = node;
        messageQueue = new ArrayDeque<>();
    }

    @Override
    public void run() {
        if (!stop.get()){
            //trigger the recovery action if the ack is not received
            ackType.recovery(node, messageQueue);

            //TODO: close the linked socket
        }
    }

    public void stop() {
        stop.set(true);
    }

    public void enqueue(Request request) {
        messageQueue.addLast(request);
    }

    public void addAckType(Ack ackType) {
        this.ackType = ackType;
    }
}
