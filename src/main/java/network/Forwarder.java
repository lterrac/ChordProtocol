package network;


import network.requests.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

import static utilities.Utilities.sha1;

/**
 * This class is responsible of sending file to other nodes.
 */
public class Forwarder implements Runnable {

    /**
     * Logger used for debug purpose
     */
    private final Logger LOGGER = Logger.getLogger(Forwarder.class.getName());
    /**
     * Contains all the sockets of the nodes connected with the current node
     */
    private final Map<String, ClientSocket> socketMap;
    /**
     * Keeps track of the last time a socket has been used
     */
    private final Map<String, Long> lastMessage;
    /**
     * Used to enable log
     */
    private boolean debug = false;
    /**
     * Encapsulates the current socket used to communicate with the target node
     */
    private ClientSocket clientSocket;

    public Forwarder(int nodeId) {
        socketMap = new HashMap<>();
        lastMessage = new HashMap<>();

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

    /**
     * Executes {@link #checkUnusedSockets()}
     */
    @Override
    public void run() {
        checkUnusedSockets();
    }

    /**
     * Main method of the class: it is used to send a {@link network.requests.Request} through a socket.
     * First of all, {@link #socketMap} is checked to see if a socket with the target node has been created.
     * If it already exists, the socket is assigned to {@link #clientSocket} and the {@link network.requests.Request}
     * is written into its output stream. Otherwise the socket is created and added to {@link #socketMap},
     * then it proceeds as descripted before. After the {@link network.requests.Request} is sent {@link #lastMessage}
     * is updated with the actual timestamp
     *
     * @param ip      Ip of the target node
     * @param port    Port of the target node
     * @param request Request to send
     */
    public synchronized void makeRequest(String ip, int port, Request request) {

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

    /**
     * Writes a {@link network.requests.Request} into the output stream of {@link #clientSocket}.
     * @param request Request to send
     */
    public void request(Request request) {

        try {
            clientSocket.getOutputStream().writeObject(request);
            clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            System.exit(0);
        }
    }

    /**
     * Update last time a message is sent to a specific client
     *
     * @param ip Ip of the node
     * @param port Port of the node
     */
    private void updateLastMessage(String ip, int port) {
        synchronized (lastMessage) {
            lastMessage.replace(ip + ":" + port, new Date().getTime());
        }

    }

    /**
     * Add a new node to the active socket to check its timestamp
     *
     * @param ip Ip of the node
     * @param port Port of the node
     */
    private void addToLastMessage(String ip, int port) {
        synchronized (lastMessage) {
            lastMessage.put(ip + ":" + port, new Date().getTime());
        }
    }


    /**
     * Checks for unused sockets. During the execution of the program the fingers of a
     * node may change several times. So it is necessary to check whether a socket is still used or not in order to
     * avoid port exhaustion.
     * To do this kind of check close the sockets inactive for more than 10 seconds.
     */
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

    /**
     * Close all the sockets and clear all the Hashmaps.
     */
    public void stop() {

        //Close the sockets
        socketMap.values().forEach(ClientSocket::close);

        //Clear the Maps
        socketMap.clear();
        lastMessage.clear();
    }
}
