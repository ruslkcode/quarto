package server;

import networking.SocketServer;
import protocol.Protocol;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GameServer extends SocketServer {
    private final ArrayList<ClientHandler> clients = new ArrayList<>();
    private final ArrayList<ClientHandler> waitingPlayers = new ArrayList<>();

    private final Map<ClientHandler, GameSession> activeSessions = new HashMap<>();

    private FileStorage storage;

    private int nextGameId = 1;

    protected GameServer(int port) throws IOException {
        super(port);
        this.storage = new FileStorage();
    }


    @Override
    protected void handleConnection(Socket socket) throws IOException {
        ClientHandler clientHandler = new ClientHandler(socket, this);
        synchronized (clients) {
            clients.add(clientHandler);
        }
        clientHandler.start();
    }

    public synchronized void handleMove(ClientHandler player, int nextPiece, int location) {
        if (!activeSessions.containsKey(player)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Not in game");
            return;
        }

        GameSession session = activeSessions.get(player);
        session.handleMove(player, nextPiece, location);
    }

    public synchronized boolean isLoggedIn(String player) {
        for (ClientHandler client : clients) {
            if (player.equals(client.getUsername())) {
                return true;
            }
        }
        return false;
    }

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

    public synchronized void checkQueue() {
        ClientHandler player1 = null;
        ClientHandler player2 = null;
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < waitingPlayers.size(); i++) {
            ClientHandler p1Candidate = waitingPlayers.get(i);
            long waitTime = currentTime - p1Candidate.getQueueJoinTime();
            int currentDiff = 500 + (int) ((waitTime / 20000) * 500);

            for (int j = i + 1; j < waitingPlayers.size(); j++) {
                ClientHandler p2Candidate = waitingPlayers.get(j);


                if (Math.abs(storage.getMmr(p1Candidate.getUsername()) - storage.getMmr(p2Candidate.getUsername())) <= currentDiff) {
                    player1 = p1Candidate;
                    player2 = p2Candidate;
                    break;
                }
            }
            if (player1 != null && player2 != null) {
                waitingPlayers.remove(player1);
                waitingPlayers.remove(player2);

                GameSession session = new GameSession(player1, player2, nextGameId++);
                activeSessions.put(player1, session);
                activeSessions.put(player2, session);
                session.startGame();

                checkQueue();
            }
        }
    }



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

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendPacket(message);
        }
    }

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

    public synchronized void endSession(ClientHandler player1, ClientHandler player2) {
        if (activeSessions.containsKey(player1)) {
            activeSessions.remove(player1);
        }
        if (activeSessions.containsKey(player2)) {
            activeSessions.remove(player2);
        }
        System.out.println("Session ended. Players " + player1.getUsername() + " and " + player2.getUsername() + " are free.");
    }


    public void updateMmr(String username, int points) {
        storage.updateMmr(username, points);
    }

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