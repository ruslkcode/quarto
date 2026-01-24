package client;

import gameLogic.Game;
import gameLogic.Move;
import protocol.Protocol;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Text-based user interface for the Quarto game.
 * and is driven entirely by server messages.
 */
public class QuartoTUI implements QuartoClient.GameListener {

    private QuartoClient client;
    private AbstractClient aiClient;
    private HumanClient humanClient;

    private Game localGame;
    private Scanner scanner;
    private String username;

    private final int[] visualBoard = new int[16];
    private int pieceInHand = -1;

    private boolean isAiMode = false;
    private boolean isGameActive = false;
    private volatile boolean isMyTurn = false;
    private volatile boolean waitingForServerEcho = false;

    /*@
      invariant visualBoard.length == 16;
    @*/

    /**
     * Starts the Quarto text user interface.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        new QuartoTUI().start();
    }

    /**
     * Initializes the client, connects to the server,
     * and starts either human or AI interaction mode.
     */
    public void start() {
        scanner = new Scanner(System.in);
        client = new QuartoClient();
        Arrays.fill(visualBoard, -1);

        System.out.print("Enter Username: ");
        username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "Player" + (int) (Math.random() * 100);
        }

        System.out.println("Select Player Type:");
        System.out.println("1. Human");
        System.out.println("2. AI");

        isAiMode = scanner.nextLine().trim().equals("2");

        if (isAiMode) {
            setupAI();
        } else {
            humanClient = new HumanClient(username, scanner);
        }

        System.out.print("Server port (Enter for 5432): ");
        int port = 5432;
        try {
            String p = scanner.nextLine().trim();
            if (!p.isEmpty()) {
                port = Integer.parseInt(p);
            }
        } catch (Exception ignored) {}

        try {
            client.connect("localhost", port, this);
            client.login(username);

            if (isAiMode) {
                client.queue();
                while (true) {
                    Thread.sleep(1000);
                }
            } else {
                humanCommandLoop();
            }

        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    /**
     * Configures the AI client and selects a strategy.
     */
    private void setupAI() {
        System.out.println("Select AI Strategy:");
        System.out.println("1. Naive");
        System.out.println("2. Smart");

        BotStrategy strategy =
                scanner.nextLine().trim().equals("2")
                        ? new SmartStrategy()
                        : new NaiveStrategy();

        aiClient = new AIClient(username, strategy, 1000);
        System.out.println("AI configured: " + strategy.getName());
    }

    /**
     * Processes console commands for a human player.
     */
    private void humanCommandLoop() {
        while (true) {
            String cmd = scanner.nextLine().trim().toLowerCase();
            switch (cmd) {
                case "queue" -> client.queue();
                case "list" -> client.listPlayers();
                case "quit" -> {
                    client.close();
                    System.exit(0);
                }
                default -> System.out.println("Commands: queue | list | quit");
            }
        }
    }

    /**
     * Called when a connection to the server is established.
     */
    @Override
    public void onConnected() {
        System.out.println("Connected to server.");
    }

    /**
     * Initializes a new game received from the server.
     *
     * @param p1 first player
     * @param p2 second player
     */
    @Override
    public void onNewGame(String p1, String p2) {
        localGame = new Game(1);
        Arrays.fill(visualBoard, -1);
        pieceInHand = -1;

        isGameActive = true;
        isMyTurn = false;
        waitingForServerEcho = false;

        drawBoard();

        if (p1.equals(username)) {
            isMyTurn = true;
            triggerMyMove();
        } else {
            System.out.println("Opponent starts.");
        }
    }

    /**
     * Handles both opponent moves and server echoes of own moves.
     *
     * @param location board index, or -1 if no placement
     * @param piece next piece ID
     */
    @Override
    public void onOpponentMove(int location, int piece) {
        if (localGame == null) {
            return;
        }

        if (location == -1) {
            localGame.doMove(new Move(piece));
        } else {
            int placed = localGame.getCurrentPieceID();
            localGame.doMove(new Move(piece, location));
            visualBoard[location] = placed;
        }

        if (waitingForServerEcho) {
            waitingForServerEcho = false;
            isMyTurn = false;
            drawBoard();
            return;
        }

        pieceInHand = piece;
        isMyTurn = true;
        drawBoard();
        triggerMyMove();
    }

    /**
     * Triggers a move if it is currently this client's turn.
     */
    private void triggerMyMove() {
        if (!isGameActive || !isMyTurn || waitingForServerEcho) {
            return;
        }

        if (isAiMode) {
            makeAiMove();
        } else {
            makeHumanMove();
        }
    }

    /**
     * Determines and sends a move for a human player.
     */
    private void makeHumanMove() {
        new Thread(() -> {
            try {
                Move move = humanClient.determineMove(localGame);
                if (move == null) {
                    return;
                }
                waitingForServerEcho = true;
                client.sendMove(move.getLocation(), move.getNextPiece());
            } catch (Exception e) {
                waitingForServerEcho = false;
            }
        }).start();
    }

    /**
     * Determines and sends a move for the AI player.
     */
    private void makeAiMove() {
        if (waitingForServerEcho) {
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Move move = aiClient.determineMove(localGame);
                if (move == null) {
                    return;
                }
                waitingForServerEcho = true;
                client.sendMove(move.getLocation(), move.getNextPiece());
                System.out.println("AI made a move.");
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Handles the end of a game.
     *
     * @param result game result
     * @param winner winner username
     */
    @Override
    public void onGameOver(String result, String winner) {
        isGameActive = false;
        isMyTurn = false;
        waitingForServerEcho = false;
        localGame = null;

        if (Protocol.VICTORY.equals(result)) {
            System.out.println(
                    winner.equals(username)
                            ? "Victory!"
                            : "Defeat. Winner: " + winner
            );
        } else {
            System.out.println("Draw game.");
        }

        if (isAiMode) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    client.queue();
                } catch (Exception ignored) {}
            }).start();
        } else {
            System.out.println("Type 'queue' to play again.");
        }
    }

    /**
     * Handles an error message from the server.
     *
     * @param msg error message
     */
    @Override
    public void onError(String msg) {
        System.out.println("Server error: " + msg);
        waitingForServerEcho = false;
    }

    /**
     * Handles a chat message.
     *
     * @param sender message sender
     * @param text message text
     */
    @Override
    public void onChat(String sender, String text) {
        if (!isAiMode) {
            System.out.println(sender + ": " + text);
        }
    }

    /**
     * Renders the board and all available pieces.
     */
    private void drawBoard() {
        System.out.println("\n      0     1     2     3");
        System.out.println("   ╔═════╦═════╦═════╦═════╗");

        for (int row = 0; row < 4; row++) {
            System.out.print("   ║");
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                int val = visualBoard[index];
                if (val == -1) {
                    System.out.printf(" %3d ║", index);
                } else {
                    System.out.printf(" [%2d]║", val);
                }
            }
            System.out.println();
            if (row < 3) {
                System.out.println("   ╠═════╬═════╬═════╬═════╣");
            } else {
                System.out.println("   ╚═════╩═════╩═════╩═════╝");
            }
        }

        if (localGame != null) {
            Set<Integer> available =
                    new TreeSet<>(localGame.getAvailablePieces().keySet());
            if (pieceInHand != -1) {
                available.remove(pieceInHand);
            }

            System.out.println("\nAvailable pieces:");
            for (int id : available) {
                System.out.print("[" + id + "] ");
            }
            System.out.println();
        }
    }
}
