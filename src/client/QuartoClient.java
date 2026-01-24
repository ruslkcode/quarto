package client;

import protocol.Protocol;
import java.io.*;
import java.net.Socket;

/**
 * Handles network communication with the Quarto Server.
 * Parses incoming messages and notifies the TUI via the GameListener interface.
 */
public class QuartoClient {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private GameListener listener;
    private volatile boolean running = false;

    /**
     * Listener interface used by the TUI.
     */
    public interface GameListener {
        void onConnected();
        void onNewGame(String p1, String p2);
        void onOpponentMove(int location, int piece);
        void onGameOver(String result, String winner);
        void onError(String msg);
        void onChat(String sender, String text);
    }

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
     * Main network listening loop.
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

    // ================== Sending ==================

    public void login(String username) {
        send(Protocol.LOGIN + Protocol.SEPARATOR + username);
    }

    public void queue() {
        send(Protocol.QUEUE);
    }

    public void sendMove(int location, int nextPiece) {
        if (!running) return;

        if (location == -1) {
            send(Protocol.MOVE + Protocol.SEPARATOR + nextPiece);
        } else {
            send(Protocol.MOVE + Protocol.SEPARATOR + location + Protocol.SEPARATOR + nextPiece);
        }
    }

    public void listPlayers() {
        send(Protocol.LIST);
    }

    public void close() {
        shutdown();
    }

    /**
     * Sends a message to the server if the connection is still alive.
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
     * Gracefully shuts down the client and releases resources.
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
     */
    private void handleFatalError(String message) {
        if (listener != null) {
            listener.onError(message);
        }
        shutdown();
    }
}
