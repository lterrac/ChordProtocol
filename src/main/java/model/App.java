package model;

import java.util.Scanner;

import static model.NodeProperties.KEY_SIZE;

public class App {

    public static void main(String[] args) {

        Node node;

        Scanner in = new Scanner(System.in);

        System.out.println("Enter \"new\" if you are the Net Generator, otherwise enter IP and port (\"IP\":\"port\") of the Node you know.");

        String input = in.nextLine();
        if (input.equals("new")) {
            node = new Node();
            node.create();
        } else {
            String[] parts = input.split(":");
            String knownIp = parts[0];
            int knownPort = Integer.parseInt(parts[1]);

            System.out.println(knownIp);
            System.out.println(knownPort);

            node = new Node();
            node.join(knownIp, knownPort);
        }

        // Workflow for demo
        int action;

        do {
            displayChoices();
            action = in.nextInt();
            switch (action) {
                case 1:
                case 2:
                case 3:
                case 4:
                    System.out.println("Finger Table");
                    for (int i = 0; i < KEY_SIZE - 1; i++) {
                        if (node.getFingers()[i] != null)
                            System.out.println(node.getFingers()[i].getNodeId());
                        else
                            System.out.println("-");
                    }
                    break;
                case 5: // Look for a key
                    System.out.println("Insert the key you are looking for (it must be in the range [0," + KEY_SIZE + "]):");
                    int key = in.nextInt();
                    String nodeIp = node.lookup(key);
                    if (nodeIp != null) {
                        System.out.println("The resource is kept by node " + nodeIp + ".");
                    } else {
                        System.out.println("The resource doesn't exist in the net.");
                    }
                    break;
                case 6: // Ping request
                    // Burn the newline character
                    in.nextLine();

                    System.out.println("Insert ip and port (\"IP\":\"port\") of the node you want to reach:");
                    String address = in.nextLine();
                    String[] parts = address.split(":");
                    String ip = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    node.forward(node.getProperties(), ip, port, "ping", 0, 0, 0);
                    break;
                case 0: // Leave the network
                    System.out.println("The node has left the network!");
                    // TODO: without this the process is kept alive
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice! Try again...");
            }
        } while (action != 0);
    }

    private static void displayChoices() {
        System.out.println("Select your choice:");
        System.out.println("1. Own IP address and ID");
        System.out.println("2. The IP address and ID of the successor and predecessor;");
        System.out.println("3. The file key IDs contained by the current node;");
        System.out.println("4. Own finger table");
        System.out.println("5. Lookup for a resource;");
        System.out.println("6. Ping a node;");
        System.out.println("0. Leave the network");
    }
}
