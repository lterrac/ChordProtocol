package model;

import network.requests.RequestWithAck;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class AckTimer extends TimerTask {

    private final AtomicBoolean stop;
    private Node node;
    private Deque<RequestWithAck> messageQueue;

    public AckTimer(Node node) {
        System.out.println("Create timer");
        this.stop = new AtomicBoolean(false);
        this.node = node;
        messageQueue = new ArrayDeque<>();
    }

    /**
     * If an Ack is not received for every message in the queue call the method recovery
     */
    @Override
    public void run() {
        System.out.println("exec");
        if (!stop.get()){
            messageQueue.forEach(requestWithAck -> requestWithAck.getAck().recovery(node, requestWithAck));
        }
    }

    public void stop() {
        System.out.println("defuse");
        stop.set(true);
    }

    public void enqueue(RequestWithAck request) {
        messageQueue.addLast(request);
    }
}
