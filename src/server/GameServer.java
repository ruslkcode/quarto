package server;

import gameLogic.Game;
import gameLogic.Move;
import networking.SocketServer;
import protocol.Protocol;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;

public class GameServer extends SocketServer {
    private Map<String, Game> activeplayers;
    private ArrayList<ClientHandler> waitingplayers = new ArrayList<>();
    private Map<ClientHandler, Game> activeGames = new HashMap<>();
    private ArrayList<ClientHandler> clients;
    private ClientHandler clientHandler;


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
        ClientHandler clientHandler = new ClientHandler(socket, this);
        clientHandler.start();
    }

    public void revcieveGame(Socket socket){

    }

    public void handleMove(ClientHandler player, int location, int nextPiece){
        if (!activeGames.containsKey(player)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Not in game");
            return;
        }

        Game game = activeGames.get(player);

        Move move;

        if (location == -1){
            move = new Move(nextPiece);
        }
        else {
            move = new Move(nextPiece, location);
        }

        boolean success = game.isValidMove(move);

        if (success) {
            player.sendPacket("MOVE IS VALID");
            ClientHandler opp = player.getOpponent();
            if (opp != null) {
                opp.sendPacket(Protocol.MOVE + Protocol.SEPARATOR + nextPiece + Protocol.SEPARATOR + location);
            }
        }
        else {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Invalid Move");
        }
    }



    public synchronized boolean isLoggedIn(String  player) {
        for (ClientHandler client : clients) {
            if (player.equals(client.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addToQueue(ClientHandler player){
        if (activeplayers.containsKey(player) || waitingplayers.contains(player)){
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Already in game or queue");
            return;
        }
        System.out.println(player + " is added to queue");
        waitingplayers.add(player);
        player.sendPacket(Protocol.QUEUE + Protocol.SEPARATOR + "WAITING");

        checkQueue();
    }


    public void checkQueue() {
        if (waitingplayers.size() >= 2){
            ClientHandler p1 = waitingplayers.remove(0);
            ClientHandler p2 = waitingplayers.remove(0);
            p1.setPlayerID(0);
            p2.setPlayerID(1);
            int startingPlayerID = new java.util.Random().nextInt(2);
            Game game = new Game(startingPlayerID);

            activeGames.put(p1, game);
            activeGames.put(p2, game);

            p1.setOpponent(p2);
            p2.setOpponent(p1);

            p1.sendPacket(Protocol.NEWGAME + Protocol.SEPARATOR + p2.getUsername() + Protocol.SEPARATOR + "0");
            p2.sendPacket(Protocol.NEWGAME + Protocol.SEPARATOR + p1.getUsername() + Protocol.SEPARATOR + "1");

            System.out.println("GAME STARTED");

        }
    }

    public boolean inGame(ClientHandler username){
        if (activeplayers.containsKey(username)){
            return true;
        }
        else return false;
    }
    public synchronized void handleDisconnect(ClientHandler player) {
        if (waitingplayers.remove(player)) {
            System.out.println(player.getUsername() + " удален из очереди.");
            return;
        }

        if (activeGames.containsKey(player)) {
            Game game = activeGames.get(player);
            ClientHandler opponent = player.getOpponent();

            System.out.println("Opponent has disconnected");

            if (opponent != null) {
                opponent.sendPacket(Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.VICTORY);
                activeGames.remove(opponent);
                opponent.setOpponent(null);
            }

            activeGames.remove(player);
        }
    }

    public void close(){
        super.close();
    }
}
