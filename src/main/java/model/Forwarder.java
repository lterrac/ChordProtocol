package model;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import java.util.stream.Collectors;

import static utilities.Utilities.sha1;

public class Forwarder implements Runnable {
    private final Logger LOGGER = Logger.getLogger(Forwarder.class.getName());
    private ClientSocket clientSocket;
    private final Map<String, ClientSocket> socketMap;
    private final Map<String, Long> lastMessage;
    private AtomicBoolean stop;
    private boolean debug = false;

    public Forwarder(int nodeId) {
        socketMap = new HashMap<>();
        lastMessage = new HashMap<>();
        stop = new AtomicBoolean(false);

        if (debug) {
            Handler consoleHandler = null;
            Handler fileHandler = null;
            try {
                //Creating consoleHandler and fileHandler
                consoleHandler = new ConsoleHandler();
                fileHandler = new FileHandler("./node-" + nodeId + ".log", 1024 * 1024, 1, true);

                //Assigning handlers to LOGGER object
                LOGGER.addHandler(consoleHandler);
                LOGGER.addHandler(fileHandler);

                //Setting levels to handlers and LOGGER
                consoleHandler.setLevel(Level.ALL);
                consoleHandler.setFormatter(new SimpleFormatter());
                fileHandler.setLevel(Level.ALL);
                LOGGER.setLevel(Level.ALL);
                fileHandler.setFormatter(new SimpleFormatter());
                LOGGER.config("Configuration done.");

                //Console handler removed
                LOGGER.removeHandler(consoleHandler);

                LOGGER.log(Level.FINE, "Finer logged");
            } catch (IOException exception) {
                LOGGER.log(Level.SEVERE, "Error occur in FileHandler.", exception);
            }

            LOGGER.finer("Finest example on LOGGER handler completed.");
        }
    }


    @Override
    public void run() {
        checkUnusedSockets();
    }

    public synchronized void makeRequest(String ip,int port,Request request) {



        try {
            ObjectOutputStream out;
            ObjectInputStream in = null;

            if (!socketMap.containsKey(ip + ":" + port)) {
                Socket socket = new Socket();
                socket.setReuseAddress(true);
                //Create a connection with the other node
                socket.connect(new InetSocketAddress(ip, port));

                //Get the streams
                out = new ObjectOutputStream(socket.getOutputStream());
                //   in = new ObjectInputStream(socket.getInputStream());

                //Create the wrapper for the socket and the streams
                clientSocket = new ClientSocket(socket, in, out);

                //Add the wrapper to the Hashmaps
                socketMap.put(ip + ":" + port, clientSocket);
                addToLastMessage(ip, port);
            }

            //Update the last time a message is sent
            updateLastMessage(ip, port);

            this.clientSocket = socketMap.get(ip + ":" + port);

            //Create the message and send it
            //Message msg = new Message(nodeInformation, message, fixId, fixIndex, lookupKey, file);

            /*if (debug)
                LOGGER.log(Level.FINE, "Target: " + sha1(ip + ":" + port) + " message:" + message);*/


            request(request);
            /*clientSocket.getOutputStream().writeObject(msg);
            clientSocket.getOutputStream().flush();
            clientSocket = null;*/

        } catch (IOException e) {
            //If a socket is no more active, close it and remove it from HashMaps
            e.printStackTrace();
            lastMessage.entrySet().removeIf(longStringEntry -> (ip + ":" + port).equals(longStringEntry.getKey()));
            socketMap.remove(ip + ":" + port);
            clientSocket.close();

        }
    }

    public void request(Request request) {

        try {
            clientSocket.getOutputStream().writeObject(request);
            clientSocket.getOutputStream().reset();
        } catch (IOException e) {
            System.exit(0);
        }
    }

    /**
     * Update last time a message is sent to a specific client
     *
     * @param ip
     * @param port
     */
    private void updateLastMessage(String ip, int port) {
        synchronized (lastMessage) {
            lastMessage.replace(ip + ":" + port, new Date().getTime());
        }

    }

    /**
     * Add a new node to the active socket to check its timestamp
     *
     * @param ip
     * @param port
     */
    private void addToLastMessage(String ip, int port) {
        synchronized (lastMessage) {
            lastMessage.put(ip + ":" + port, new Date().getTime());
        }
    }


    private void checkUnusedSockets() {

        List<String> socketToClose;
        List<ClientSocket> sockets = new ArrayList<>();

        synchronized (lastMessage) {
            //Find the sockets unused
            socketToClose = lastMessage.entrySet().stream()
                    .filter(ts -> (new Date().getTime() - ts.getValue()) > 10000)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            //Delete them from lastMessage
            socketToClose.forEach(lastMessage::remove);
        }

        if (debug) {
            LOGGER.log(Level.SEVERE, "\tSocket to close");
            StringBuilder stringBuilder = new StringBuilder();
            socketToClose.forEach(s -> {
                stringBuilder.append("\t");
                stringBuilder.append(sha1(s));
            });
            LOGGER.log(Level.SEVERE, stringBuilder.toString());

            LOGGER.log(Level.SEVERE, "TOTAL SOCKET = " + socketMap.size());
            StringBuilder stringBuilder2 = new StringBuilder();
            socketMap.keySet().forEach(s -> {
                stringBuilder2.append("\t");
                stringBuilder2.append(sha1(s));
            });
            LOGGER.log(Level.SEVERE, stringBuilder2.toString());
        }


        synchronized (socketMap) {


            //Remove the sockets from the socketMap and collect them to delete them
            socketToClose.forEach(s -> sockets.add(socketMap.remove(s)));

            //Close the sockets
            for (ClientSocket socket1 : sockets) {
                socket1.close();
            }
        }
    }


    public void stop() {

        //Close the sockets
        socketMap.values().forEach(ClientSocket::close);

        //Clear the Maps
        socketMap.clear();
        lastMessage.clear();
    }
}
