package server;

import gameLogic.Game;

public class ClientHandler {

    private final ServerConnection connection;
    private String username;
    private GameServer server;

    /**
     * Constructs the ClientHandler.
     * @param connection is the connection.
     */
    public ClientHandler(ServerConnection connection, GameServer server) {
        this.connection = connection;
        this.server = server;

    }

    public String getUsername() {
        return username;
    }

    /**
     * The method that receives the username and checks if it already exists.
     * @param name the desired username.
     */
    public void receiveUsername(String name){
        if (username == null && name != null && !name.isEmpty()) {
            username = name;
        }
    }


    /**
     * The method that handles disconnection.
     */
    public void handleDisconnect() {
    }
}
