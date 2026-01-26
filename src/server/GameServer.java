package server;

import networking.SocketServer;
import protocol.Protocol;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * GameServer is responsible for managing client connections,
 * matchmaking, and active game sessions.
 * <p>
 * It maintains:
 * <ul>
 *   <li>a list of connected clients</li>
 *   <li>a matchmaking queue</li>
 *   <li>active game sessions</li>
 * </ul>
 */
public class GameServer extends SocketServer {

    private final ArrayList<ClientHandler> clients = new ArrayList<>();
    private final ArrayList<ClientHandler> waitingPlayers = new ArrayList<>();

    private final Map<ClientHandler, GameSession> activeSessions = new HashMap<>();
    private Map<ClientHandler, Integer> playersMmr = new HashMap<>();

    private FileStorage storage;

    private int nextGameId = 1;

    /*@
      @ private invariant clients != null;
      @ private invariant waitingPlayers != null;
      @ private invariant activeSessions != null;
      @ private invariant nextGameId > 0;
      @*/

    /**
     * Creates a new GameServer instance on the given port.
     *
     * @param port port number (0 for random available port)
     * @throws IOException if the server socket cannot be opened
     */
    protected GameServer(int port) throws IOException {
        super(port);
        this.storage = new FileStorage();
    }

    /**
     * Handles a new incoming socket connection.
     * A ClientHandler is created and started for the socket.
     *
     * @param socket the client socket
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void handleConnection(Socket socket) throws IOException {
        ClientHandler clientHandler = new ClientHandler(socket, this);
        synchronized (clients) {
            clients.add(clientHandler);
        }
        clientHandler.start();
    }

    /**
     * Handles a move sent by a player.
     * If the player is not in an active session, an error is sent.
     *
     * @param player the player making the move
     * @param nextPiece the next piece chosen
     * @param location the board location
     */
    /*@
      @ requires player != null;
      @*/
    public synchronized void handleMove(ClientHandler player, int nextPiece, int location) {
        if (!activeSessions.containsKey(player)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Not in game");
            return;
        }

        GameSession session = activeSessions.get(player);
        session.handleMove(player, nextPiece, location);
    }

    /**
     * Checks whether a user with the given username is logged in.
     *
     * @param player username to check
     * @return true if the user is logged in, false otherwise
     */
    public synchronized boolean isLoggedIn(String player) {
        for (ClientHandler client : clients) {
            if (player.equals(client.getUsername())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a player to the matchmaking queue.
     * The player cannot already be in a game or in the queue.
     *
     * @param player the player to add
     */
    /*@
      @ requires player != null;
      @*/
    public synchronized void addToQueue(ClientHandler player) {
        if (activeSessions.containsKey(player) || waitingPlayers.contains(player)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Already in game or queue");
            return;
        }

        player.setQueueJoinTime(System.currentTimeMillis());

        System.out.println(player.getUsername() + " added to queue");
        waitingPlayers.add(player);

        checkQueue();
    }

    /**
     * Checks the matchmaking queue and starts new game sessions
     * if suitable players are available.
     */
    public synchronized void checkQueue() {

        if (waitingPlayers.size() >= 2) {
            ClientHandler p1 = waitingPlayers.removeFirst();
            ClientHandler p2 = waitingPlayers.removeFirst();

            GameSession session = new GameSession(p1, p2, nextGameId++);
            activeSessions.put(p1, session);
            activeSessions.put(p2, session);

            session.startGame();

            checkQueue();
        }


        // MMR GAME SEARCH
        //        ClientHandler player1 = null;
        //        ClientHandler player2 = null;
        //        long currentTime = System.currentTimeMillis();
        //
        //        for (int i = 0; i < waitingPlayers.size(); i++) {
        //            ClientHandler p1Candidate = waitingPlayers.get(i);
        //            long waitTime = currentTime - p1Candidate.getQueueJoinTime();
        //            int currentDiff = 500 + (int) ((waitTime / 20000) * 500);
        //
        //            for (int j = i + 1; j < waitingPlayers.size(); j++) {
        //                ClientHandler p2Candidate = waitingPlayers.get(j);
        //
        //
        //                if (Math.abs(storage.getMmr(p1Candidate.getUsername()) - storage.getMmr(p2Candidate.getUsername())) <= currentDiff) {
        //                    player1 = p1Candidate;
        //                    player2 = p2Candidate;
        //                    break;
        //                }
        //            }
        //            if (player1 != null && player2 != null) {
        //                waitingPlayers.remove(player1);
        //                waitingPlayers.remove(player2);
        //
        //                GameSession session = new GameSession(player1, player2, nextGameId++);
        //                activeSessions.put(player1, session);
        //                activeSessions.put(player2, session);
        //                session.startGame();
        //
        //                checkQueue();
        //                return;
        //            }
        //        }
    }

    /**
     * Returns a protocol-formatted list of all logged-in users.
     *
     * @return list of usernames separated by protocol separator
     */
    public synchronized String getUserList() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                sb.append(client.getUsername()).append(Protocol.SEPARATOR);
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Sends a message to all connected clients.
     *
     * @param message the message to broadcast
     */
    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendPacket(message);
        }
    }

    /**
     * Handles client disconnection.
     * Removes the client from the queue or active session if needed.
     *
     * @param player the disconnected client
     */
    /*@
      @ requires player != null;
      @*/
    public synchronized void handleDisconnect(ClientHandler player) {
        if (waitingPlayers.remove(player)) {
            System.out.println(player.getUsername() + " removed from queue");
            return;
        }
        if (activeSessions.containsKey(player)) {
            GameSession session = activeSessions.get(player);
            session.disconnect(player);
            ClientHandler opponent = player.getOpponent();
            if (opponent != null) {
                activeSessions.remove(opponent);
            }
            activeSessions.remove(player);
        }
        clients.remove(player);
    }

    /**
     * Ends a game session and removes both players from active sessions.
     *
     * @param player1 first player
     * @param player2 second player
     */
    /*@
      @ requires player1 != null && player2 != null;
      @ ensures !activeSessions.containsKey(player1);
      @ ensures !activeSessions.containsKey(player2);
      @*/
    public synchronized void endSession(ClientHandler player1, ClientHandler player2) {
        if (activeSessions.containsKey(player1)) {
            activeSessions.remove(player1);
        }
        if (activeSessions.containsKey(player2)) {
            activeSessions.remove(player2);
        }
        System.out.println("Session ended. Players " + player1.getUsername() + " and " + player2.getUsername() + " are free.");
    }

    /**
     * Returns the rankings formatted for protocol communication.
     *
     * @return rankings string
     */
    public synchronized String getProtocolRankings() {
        return storage.getRankingsForProtocol();
    }

    /**
     * Updates the MMR of a player.
     *
     * @param username player username
     * @param points MMR points to add or remove
     */
    public void updateMmr(String username, int points) {
        storage.updateMmr(username, points);
    }

    /**
     * Starts the server from the command line.
     *
     * @param args command-line arguments
     * @throws IOException if server startup fails
     */
    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);
        System.out.println("Enter the port number (0 for random): ");
        String s = input.nextLine();
        int port = Integer.parseInt(s);
        GameServer server = new GameServer(port);
        System.out.println("Server is on port: " + server.getPort());
        server.acceptConnections();
    }
}
