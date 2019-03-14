package main.java.model;

import java.util.Scanner;

public class App {


    public static void main(String[] args) throws InterruptedException {
        //Fingers fingerTable;

        Node host;

        Scanner in = new Scanner(System.in);


        System.out.println("Enter -1 if you are the Net Generator, otherwise enter the port number of the Node you know.");
        int inputPort = in.nextInt();
        if(inputPort == -1) {

            do{

                System.out.println("Enter the port number you want to use.");
                int port = in.nextInt();
                System.out.println(port);
                host = new Node(port);
                Thread.sleep(400);//TODO: check correctness
            }while(!host.isFreePort());

        }
        else{
            System.out.println("Enter the ip Address of the Node you know.");
            String newLine=in.nextLine();
            String inputIp=in.nextLine();
            System.out.println(inputIp);
            host = new Node(inputPort,inputIp);

        }
    }
}
