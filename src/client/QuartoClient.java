package client;

import protocol.Protocol;
import java.io.*;
import java.net.Socket;

/**
 * Network client for communicating with the Quarto server.
 * The client runs a dedicated listener thread that processes
 * incoming messages asynchronously.
 */
public class QuartoClient {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private GameListener listener;
    private volatile boolean running = false;

    /*@
      private invariant running ==> socket != null;
    @*/

    /**
     * Callback interface implemented by the TUI.
     * Used to notify the UI about server events.
     */
    public interface GameListener {

        /** Called once the client successfully connects to the server. */
        void onConnected();

        /**
         * Called when the server starts a new game.
         *
         * @param p1 name of player 1
         * @param p2 name of player 2
         */
        void onNewGame(String p1, String p2);

        /**
         * Called when a move is received.
         *
         * @param location board index (or -1 if no placement)
         * @param piece next piece id
         */
        void onOpponentMove(int location, int piece);

        /**
         * Called when the game ends.
         *
         * @param result game result (VICTORY / DRAW)
         * @param winner winner name (empty if draw)
         */
        void onGameOver(String result, String winner);

        /**
         * Called when a protocol or connection error occurs.
         *
         * @param msg error message
         */
        void onError(String msg);

        /**
         * Called when a chat message is received.
         *
         * @param sender sender name
         * @param text message text
         */
        void onChat(String sender, String text);
    }

    /**
     * Connects to the Quarto server and starts the listener thread.
     *
     * @param host server hostname
     * @param port server port
     * @param listener UI listener for callbacks
     * @throws IOException if the connection fails
     */
    /*@
      requires host != null;
      requires port > 0;
      ensures running;
    @*/
    public void connect(String host, int port, GameListener listener) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.listener = listener;
        this.running = true;

        new Thread(this::listen, "QuartoClient-Listener").start();

        if (listener != null) {
            listener.onConnected();
        }
    }

    /**
     * Main loop that listens for incoming messages from the server.
     * Runs in a dedicated background thread.
     */
    private void listen() {
        try {
            String msg;
            while (running && (msg = in.readLine()) != null) {

                if (listener == null) continue;

                String[] parts = msg.split(Protocol.SEPARATOR);
                String cmd = parts[0];

                switch (cmd) {

                    case Protocol.NEWGAME -> {
                        String p1 = parts.length > 1 ? parts[1] : "";
                        String p2 = parts.length > 2 ? parts[2] : "";
                        listener.onNewGame(p1, p2);
                    }

                    case Protocol.MOVE -> {
                        try {
                            if (parts.length == 2) {
                                int piece = Integer.parseInt(parts[1]);
                                listener.onOpponentMove(-1, piece);
                            } else if (parts.length == 3) {
                                int loc = Integer.parseInt(parts[1]);
                                int piece = Integer.parseInt(parts[2]);
                                listener.onOpponentMove(loc, piece);
                            }
                        } catch (NumberFormatException e) {
                            handleFatalError("Malformed MOVE message");
                        }
                    }

                    case Protocol.GAMEOVER -> {
                        String result = parts.length > 1 ? parts[1] : Protocol.DRAW;
                        String winner = parts.length > 2 ? parts[2] : "";
                        listener.onGameOver(result, winner);
                    }

                    case Protocol.CHAT -> {
                        if (parts.length > 2) {
                            listener.onChat(parts[1], parts[2]);
                        }
                    }

                    case Protocol.ERROR -> {
                        String errorMsg = parts.length > 1 ? parts[1] : "Unknown error";
                        listener.onError(errorMsg);
                        shutdown();
                        return;
                    }
                }
            }
        } catch (IOException e) {
            if (running && listener != null) {
                listener.onError("Connection lost");
            }
        } finally {
            shutdown();
        }
    }

    /**
     * Sends a LOGIN command to the server.
     *
     * @param username chosen username
     */
    /*@
      requires username != null;
      requires running;
    @*/
    public void login(String username) {
        send(Protocol.LOGIN + Protocol.SEPARATOR + username);
    }

    /**
     * Sends a QUEUE command to the server.
     * Places the client into the matchmaking queue.
     */
    /*@
      requires running;
    @*/
    public void queue() {
        send(Protocol.QUEUE);
    }

    /**
     * Sends a MOVE command to the server.
     *
     * @param location board index, or -1 if no placement
     * @param nextPiece next piece id
     */
    /*@
      requires running;
    @*/
    public void sendMove(int location, int nextPiece) {
        if (!running) return;

        if (location == -1) {
            send(Protocol.MOVE + Protocol.SEPARATOR + nextPiece);
        } else {
            send(Protocol.MOVE + Protocol.SEPARATOR + location
                         + Protocol.SEPARATOR + nextPiece);
        }
    }

    /**
     * Requests a list of currently connected players.
     */
    /*@
      requires running;
    @*/
    public void listPlayers() {
        send(Protocol.LIST);
    }

    /**
     * Closes the client connection.
     */
    public void close() {
        shutdown();
    }

    /**
     * Sends a raw protocol message to the server.
     */
    private synchronized void send(String msg) {
        if (!running) return;

        try {
            out.write(msg);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            shutdown();
        }
    }

    /**
     * Gracefully shuts down the client and releases all resources.
     */
    private synchronized void shutdown() {
        if (!running) return;
        running = false;

        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
    }

    /**
     * Handles unrecoverable protocol errors.
     * @param message error description
     */
    private void handleFatalError(String message) {
        if (listener != null) {
            listener.onError(message);
        }
        shutdown();
    }
}
