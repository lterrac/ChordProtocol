package main.java.model;

import java.util.Scanner;

public class App {

    public static void main(String[] args) {

        Node host;

        Scanner in = new Scanner(System.in);

        System.out.println("Enter \"new\" if you are the Net Generator, otherwise enter IP and port (\"IP\":\"port\") of the Node you know.");

        String input = in.nextLine();
        if (input.equals("new")) {
            host = new Node();
        } else {
            String[] parts = input.split(":");
            String knownIp = parts[0];
            int knownPort = Integer.parseInt(parts[1]);

            System.out.println(knownIp);
            System.out.println(knownPort);

            //host = new Node(knownIp, knownPort);
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
                case 5:
                case 0: System.out.println("The node has left the network!");
                        System.exit(0);
                        break;
                default: System.out.println("Invalid choice! Try again...");
            }
        } while (action != 0);

        System.out.println("Bye.");
    }

    private static void displayChoices() {
        System.out.println("Select your choice:");
        System.out.println("1. Own IP address and ID");
        System.out.println("2. The IP address and ID of the successor and predecessor;");
        System.out.println("3. The file key IDs contained by the current node;");
        System.out.println("4. Own finger table");
        System.out.println("5. Lookup for a resource;");
        System.out.println("0. Leave the network");
    }
}
