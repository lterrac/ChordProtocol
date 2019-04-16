package model;

import java.io.File;
import java.util.Scanner;
import java.util.logging.Logger;

import static model.NodeProperties.KEY_SIZE;
import static utilities.Utilities.sha1;

public class App {

    private static final Logger logger = Logger.getLogger(App.class.getName()); // TODO: cancel if not necessary
    private static Node node;
    private static Scanner stringScanner = new Scanner(System.in);
    private static Scanner intScanner = new Scanner(System.in); // To avoid burning new line characters

    public static void main(String[] args) {
        System.out.println("Enter \"new\" if you are the Net Generator, otherwise enter IP and port (\"IP\":\"port\") of the Node you know.");

        String input = stringScanner.nextLine();
        if (input.equals("new")) {
            node = new Node();
            node.create();
        } else {
            String[] parts = input.split(":");
            String knownIp = parts[0];
            int knownPort = Integer.parseInt(parts[1]);
            node = new Node();
            node.join(knownIp, knownPort);
        }

        // Workflow for demo
        int action;

        displayChoices(); // TODO: cancel this line

        do {
            // displayChoices(); TODO: commented just for testing
            action = intScanner.nextInt();
            switch (action) {
                case 1: // server coordinates
                    node.printServerCoordinates();
                    break;
                case 2: // finger table
                    node.printFingerTable();
                    break;
                case 3: // predecessor and successor coordinates
                    node.printPredecessorAndSuccessor();
                    break;
                case 4: // resources contained by the node
                    printResources();
                    break;
                case 5: // Look for a key
                    lookup();
                    break;
                case 6: // Ping request
                    ping();
                    break;
                case 0: // Leave the network
                    System.out.println("The node has left the network!");
                    exit();
                    break;
                default:
                    System.out.println("Invalid choice! Try again...");
            }
        } while (action != 0);
    }

    private static void displayChoices() {
        System.out.println("Select your choice:");
        System.out.println("1. Own IP address, port of the server and ID");
        System.out.println("2. Own finger table");
        System.out.println("3. The IP address, port of the server and ID of the successor and predecessor;");
        System.out.println("4. The file key IDs and content contained by the current node;");
        System.out.println("5. Lookup for a resource;");
        System.out.println("6. Ping a node;");
        System.out.println("0. Leave the network");
    }

    private static void ping() {
        int port;

        System.out.println("Insert ip and port (\"IP\":\"port\") of the node you want to reach:");
        String address = stringScanner.nextLine();
        String[] parts = address.split(":");

        if (parts.length == 2) {
            String ip = parts[0];
            port = Integer.parseInt(parts[1]);
            node.forward(node.getProperties(), ip, port, "ping", 0, 0, 0, null);
        } else {
            System.out.println("Please check the correctness of the input and try again");
        }
        System.out.println("------------------------------------------\n");
    }

    private static void lookup() {
        System.out.println("Insert the key you are looking for (it must be in the range [0," + KEY_SIZE + "]):");
        int key = intScanner.nextInt();
        node.lookup(node.getProperties(), key);

        System.out.println("------------------------------------------\n");
    }

    private static void exit() {
        node.transferOnLeave();
        node.notifyNeighbours();
        node.close();
        System.exit(0);
    }

    private static void printResources(){
        File folder = new File("./node" + node.getProperties().getNodeId());
        File[] allFiles = folder.listFiles();

        if (allFiles != null) {
            System.out.println(allFiles.length + " resources available in node "+ node.getProperties().getNodeId() + ":");
            for (File allFile : allFiles) {
                System.out.println("SHA: " + sha1(allFile.getName()) + "\tName: " + allFile.getName());
            }
        } else {
            System.out.println("No resources available yet.");
        }
        System.out.println("------------------------------------------\n");
    }
}