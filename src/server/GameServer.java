package server;

import networking.SocketServer;

import java.io.IOException;
import java.net.Socket;

public class GameServer extends SocketServer {

    /**
     * Creates a new Server that listens for connections on the given port.
     * Use port 0 to let the system pick a free port.
     *
     * @param port the port on which this server listens for connections
     * @throws IOException if an I/O error occurs when opening the socket
     */
    protected GameServer(int port) throws IOException {
        super(port);
    }
    /**
     * Creates a new connection handler for the given socket.
     *
     * @param socket the socket for the connection
     */
    /*@ requires socket != null; @*/
    @Override
    protected void handleConnection(Socket socket) throws IOException {
        ServerConnection connection = new ServerConnection(socket);
        connection.connect();
    }

    public void close(){
        super.close();
    }
}
