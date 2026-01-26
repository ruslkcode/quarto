package server;

import networking.SocketConnection;
import protocol.Protocol;
import java.io.IOException;
import java.net.Socket;
import org.apache.commons.lang3.StringUtils;

/**
 * Handles communication with a single connected client.
 *
 * A ClientHandler is responsible for:
 * - parsing protocol messages received from the client
 * - delegating game-related actions to the GameServer
 * - maintaining client-specific state such as username and opponent
 */
public class ClientHandler extends SocketConnection {

    private String username;
    private final GameServer server;
    private ClientHandler opponent;
    private int playerID;
    private long queueJoinTime = 0;

    /*@
      private invariant server != null;
      private invariant playerID == 0 || playerID == 1 || playerID == 2;
      private invariant queueJoinTime >= 0;
    @*/

    /**
     * Creates a new ClientHandler for a connected socket.
     *
     * @param socket the socket associated with the client
     * @param server the game server managing this client
     * @throws IOException if the socket streams cannot be initialized
     */
    /*@
      requires socket != null;
      requires server != null;
      ensures this.server == server;
    @*/
    protected ClientHandler(Socket socket, GameServer server) throws IOException {
        super(socket);
        this.server = server;
    }

    /**
     * Sets the timestamp at which the client joined the queue.
     *
     * @param time time in milliseconds since epoch
     */
    /*@
      requires time >= 0;
      ensures queueJoinTime == time;
    @*/
    public void setQueueJoinTime(long time) {
        this.queueJoinTime = time;
    }

    /**
     * Returns the timestamp at which the client joined the queue.
     *
     * @return queue join time
     */
    /*@
      ensures \result >= 0;
    @*/
    public long getQueueJoinTime() {
        return this.queueJoinTime;
    }

    /**
     * Returns the username of this client.
     *
     * @return the username, or null if the client is not logged in
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the opponent of this client.
     *
     * @param opponent the opposing client
     */
    public void setOpponent(ClientHandler opponent) {
        this.opponent = opponent;
    }

    /**
     * Returns the opponent of this client.
     *
     * @return the opponent, or null if no opponent is assigned
     */
    public ClientHandler getOpponent() {
        return opponent;
    }

    /**
     * Assigns a player ID to this client.
     *
     * @param playerID the player ID (typically 1 or 2)
     */
    /*@
      requires playerID == 1 || playerID == 2;
      ensures this.playerID == playerID;
    @*/
    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    /**
     * Returns the player ID of this client.
     *
     * @return the player ID
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * Handles a single incoming protocol message from the client.
     * The message is parsed and delegated to the appropriate
     * server-side logic.
     *
     * @param message the received protocol message
     */
    /*@
      requires message != null;
    @*/
    @Override
    protected void handlePackets(String message) throws NumberFormatException {

        if (StringUtils.isAllBlank(message)) {
            return;
        }

        String[] parts = StringUtils.split(message, Protocol.SEPARATOR);
        String command = parts[0];

        try {
            switch (command) {

                case Protocol.HELLO:
                    sendPacket(Protocol.HELLO + Protocol.SEPARATOR + "Server is ready");
                    break;

                case Protocol.LOGIN:
                    if (parts.length > 1) {
                        String name = StringUtils.stripToNull(parts[1]);
                        if (name != null) {
                            if (server.isLoggedIn(name)) {
                                sendPacket(Protocol.ERROR + Protocol.SEPARATOR
                                                   + name + " is " + Protocol.ALREADYLOGGEDIN);
                            } else {
                                this.username = name;
                                sendPacket(Protocol.LOGIN + Protocol.SEPARATOR + "SUCCESS");
                                System.out.println(username + " is logged in");
                            }
                        } else {
                            sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "ERROR: EMPTY USERNAME");
                        }
                    }
                    break;

                case Protocol.QUEUE:
                    if (this.username == null) {
                        sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "YOU HAVE TO LOGIN");
                        return;
                    }
                    System.out.println(this.username + " added to queue");
                    server.addToQueue(this);
                    break;

                case Protocol.MOVE:
                    if (parts.length == 2) {
                        int piece = Integer.parseInt(parts[1]);
                        server.handleMove(this, piece, -1);
                    } else if (parts.length == 3) {
                        int location = Integer.parseInt(parts[1]);
                        int piece = Integer.parseInt(parts[2]);
                        server.handleMove(this, piece, location);
                    } else {
                        sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Invalid Move Format");
                    }
                    break;

                case Protocol.LIST:
                    String users = server.getUserList();
                    sendPacket(Protocol.LIST + Protocol.SEPARATOR + users);
                    break;

                case Protocol.CHAT:
                    if (parts.length > 1) {
                        String text = parts[1];
                        server.broadcast(
                                Protocol.CHAT + Protocol.SEPARATOR
                                        + this.username + Protocol.SEPARATOR + text
                        );
                    }
                    break;

                case Protocol.RANK:
                    String payload = server.getProtocolRankings();
                    sendPacket(Protocol.RANK + payload);
                    break;
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * Returns the server managing this client.
     *
     * @return the game server
     */
    public GameServer getServer() {
        return this.server;
    }

    /**
     * Handles client disconnection.
     * Notifies the server so that cleanup can be performed.
     */
    @Override
    public void handleDisconnect() {
        System.out.println(Protocol.DISCONNECT + Protocol.SEPARATOR + this.username);
        if (server != null) {
            server.handleDisconnect(this);
        }
    }
}
