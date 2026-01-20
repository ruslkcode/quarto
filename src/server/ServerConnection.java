package server;

import networking.SocketConnection;
import protocol.Protocol;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection extends SocketConnection {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SocketConnection connection;
    /**
     * Constructor for the Server Connection
     * @param socket socket through wich everything will be connected
     * @throws IOException
     */
    protected ServerConnection(Socket socket) throws IOException {
        super(socket);

    }

    /**
     * Handles packet that was sent to server.
     * @param message the message received from the connection
     */
    @Override
    protected void handlePackets(String message) {
        try {
            if (!(message.contains(Protocol.SEPARATOR))) {
                return;
            }
            String[] parts = message.split(Protocol.SEPARATOR, 2);
            if (parts.length < 2) {
                return;
            }
            String command = parts[0];
            String payload = parts[1];

            if (Protocol.HELLO.equals(command)) {

                return;
            }

            if (Protocol.QUEUE.equals(command)) {

            }
            if (Protocol.LOGIN.equals(command)) {

            }
            if (Protocol.MOVE.equals(command)) {

            }
            if (Protocol.NEWGAME.equals(command)) {

            }
            if (Protocol.ERROR.equals(command)) {

            }
        } catch (Exception e) {

        }
    }

    /**
     * Handles dissconection
     */
    @Override
    protected void handleDisconnect() {

    }

    public void connect(){
        super.start();
    }

    public boolean sendPacket(String message){
        return connection.sendPacket(message);
    }

    public void recievePacket(){
        connection.receivePackets();
    }

    public void close(){
        connection.close();
    }

}
