package model;

import network.requests.Request;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class AckTimer extends TimerTask {

    private final AtomicBoolean stop;
    private Node node;
    private Request request;

    AckTimer(Node node, Request request) {
        this.stop = new AtomicBoolean(false);
        this.node = node;
        this.request = request;
    }

    @Override
    public void run() {
        if (!stop.get()){
            node.retryAndUpdate(request);
        }
    }

    void stop() {
        stop.set(true);
    }
}
