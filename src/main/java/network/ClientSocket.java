package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientSocket {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientSocket(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
