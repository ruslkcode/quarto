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
    private boolean running = false;

    // Interface to notify TUI about game events
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

        new Thread(this::listen).start();
        if (listener != null) listener.onConnected();
    }

    private void listen() {
        try {
            String msg;
            while (running && (msg = in.readLine()) != null) {
                if (listener == null) continue;

                String[] parts = msg.split(Protocol.SEPARATOR);
                String cmd = parts[0];

                switch (cmd) {
                    case Protocol.NEWGAME:
                        String p1 = (parts.length > 1) ? parts[1] : "";
                        String p2 = (parts.length > 2) ? parts[2] : "";
                        listener.onNewGame(p1, p2);
                        break;

                    case Protocol.MOVE:
                        // MOVE~LOC~PIECE or MOVE~PIECE (first move)
                        if (parts.length == 2) {
                            int piece = Integer.parseInt(parts[1]);
                            listener.onOpponentMove(-1, piece);
                        } else if (parts.length == 3) {
                            int loc = Integer.parseInt(parts[1]);
                            int piece = Integer.parseInt(parts[2]);
                            listener.onOpponentMove(loc, piece);
                        }
                        break;

                    case Protocol.GAMEOVER:
                        String res = (parts.length > 1) ? parts[1] : "DRAW";
                        String win = (parts.length > 2) ? parts[2] : "";
                        listener.onGameOver(res, win);
                        break;

                    case Protocol.CHAT:
                        if (parts.length > 2) listener.onChat(parts[1], parts[2]);
                        break;

                    case Protocol.ERROR:
                        listener.onError((parts.length > 1) ? parts[1] : "Unknown");
                        break;
                }
            }
        } catch (IOException e) {
            if (running) listener.onError("Connection lost: " + e.getMessage());
        }
    }

    // --- Sending Methods ---

    public void login(String username) {
        send(Protocol.LOGIN + Protocol.SEPARATOR + username);
    }

    public void queue() {
        send(Protocol.QUEUE);
    }

    public void sendMove(int location, int nextPiece) {
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
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }

    private void send(String msg) {
        try {
            out.write(msg);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}