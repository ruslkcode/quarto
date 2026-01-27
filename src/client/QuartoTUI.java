package client;

import gameLogic.Game;
import gameLogic.Move;
import protocol.Protocol;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Text-based User Interface for the Quarto client.
 */
public class QuartoTUI implements QuartoClient.GameListener {

    private QuartoClient client;
    private AbstractClient aiClient;
    private Game localGame;
    private String username;
    private Scanner scanner;

    /** Local visual representation of the board (indices 0–15). */
    private int[] visualBoard = new int[16];

    /** Piece currently held by this player (-1 if none). */
    private int pieceInHand = -1;

    private boolean isAiMode = false;
    private boolean isGameActive = false;

    /**
     * Turn-handling flags.
     *
     * <p>{@code isMyTurn} — indicates whether this client may perform a move.</p>
     * <p>{@code waitingForServerEcho} — prevents double input until server confirms move.</p>
     */
    private volatile boolean isMyTurn = false;
    private volatile boolean waitingForServerEcho = false;

    public static void main(String[] args) {
        new QuartoTUI().start();
    }

    /**
     * Initializes the client, asks for configuration, and connects to the server.
     */
    public void start() {
        scanner = new Scanner(System.in);
        client = new QuartoClient();
        Arrays.fill(visualBoard, -1);

        System.out.println("╔════════════════════════════╗");
        System.out.println("║       QUARTO CLIENT        ║");
        System.out.println("╚════════════════════════════╝");

        System.out.print("Enter Username: ");
        username = scanner.nextLine();
        if (username.isBlank()) username = "Player" + (int)(Math.random() * 100);

        System.out.println("\nSelect Player Type:");
        System.out.println("1. Human");
        System.out.println("2. AI (Bot)");
        System.out.print("> ");
        String type = scanner.nextLine();

        if (type.equals("2")) {
            isAiMode = true;
            setupAI();
        } else {
            isAiMode = false;
            System.out.println("✅ Mode: HUMAN");
        }

        System.out.print("Server Port (Enter for 5432): ");
        int port = 5432;
        try {
            String input = scanner.nextLine();
            if (!input.isBlank()) port = Integer.parseInt(input);
        } catch (Exception ignored) {}

        try {
            client.connect("localhost", port, this);
            client.send("HELLO~reference");
            client.login(username);

            if (isAiMode) {
                System.out.println("🤖 Bot started. Auto-queueing...");
                Thread.sleep(500);
                client.queue();

                // Keep AI client alive
                while (true) {
                    Thread.sleep(1000);
                }
            } else {
                mainInputLoop();
            }

        } catch (Exception e) {
            System.out.println("❌ Connection Error: " + e.getMessage());
        }
    }

    /**
     * Configures AI strategy and initializes the AI client.
     */
    private void setupAI() {
        System.out.println("Select AI Strategy:");
        System.out.println("1. Naive (Random)");
        System.out.println("2. Smart (Minimax/Heuristic)");
        System.out.print("> ");
        String choice = scanner.nextLine();

        BotStrategy strategy;
        if (choice.equals("2")) strategy = new SmartStrategy();
        else strategy = new NaiveStrategy();

        // Small delay to make AI behavior feel human-like
        aiClient = new AIClient(username, strategy, 1000);
        System.out.println("✅ AI Configured: " + strategy.getName());
    }

    /**
     * Main input loop for human players.
     * Processes commands and move input depending on game state.
     */
    private void mainInputLoop() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("Commands: queue | rank | quit ");

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            // Only allow move input if it is our turn
            if (isGameActive && isMyTurn && !waitingForServerEcho) {
                processHumanMoveInput(line);
                continue;
            }

            String cmd = line.split(" ")[0].toLowerCase();
            switch (cmd) {
                case "queue": client.queue(); break;
                case "list": client.listPlayers(); break;
                case "rank": client.rankList(); break;
                case "quit":
                    client.close();
                    System.exit(0);
                    break;
                default:
                    if (isGameActive) {
                        if (waitingForServerEcho)
                            System.out.println("⚠️ Waiting for server confirmation...");
                        else
                            System.out.println("⚠️ Not your turn! Opponent is thinking.");
                    } else {
                        System.out.println("Unknown command.");
                    }
            }
        }
    }

    /**
     * Parses and validates move input from a human player.
     *
     * @param line raw input line
     */
    private void processHumanMoveInput(String line) {
        try {
            String[] parts = line.split("\\s+");

            if (pieceInHand == -1) {
                if (parts.length != 1) {
                    System.out.println("❌ Invalid format. Enter: <PIECE_TO_GIVE>");
                    return;
                }

                int pieceToGive = Integer.parseInt(parts[0]);
                if (!isValidPiece(pieceToGive)) {
                    System.out.println("❌ Invalid Piece ID (must be available).");
                    return;
                }

                client.sendMove(-1, pieceToGive);
                waitingForServerEcho = true;
                System.out.println("⏳ Sending move...");
                return;
            }

            if (parts.length != 2) {
                System.out.println("❌ Format: <LOCATION> <PIECE_TO_GIVE>");
                System.out.println("   Or: <LOCATION> 16 (for Victory)");
                return;
            }

            int loc = Integer.parseInt(parts[0]);
            int code = Integer.parseInt(parts[1]); // Piece ID or special code

            // Validate board location
            if (loc < 0 || loc > 15 || visualBoard[loc] != -1) {
                System.out.println("❌ Invalid Location.");
                return;
            }

            // Allow special protocol codes or valid pieces
            boolean isSpecialCode = (code == 16 || code == 17);
            if (!isSpecialCode && !isValidPiece(code)) {
                System.out.println("❌ Invalid Piece ID or Code.");
                return;
            }

            client.sendMove(loc, code);
            waitingForServerEcho = true;

            if (code == 16) System.out.println("🏆 Claiming VICTORY (16)...");
            else if (code == 17) System.out.println("🤝 Claiming DRAW (17)...");
            else System.out.println("⏳ Sending move...");

        } catch (NumberFormatException e) {
            System.out.println("❌ Numbers only.");
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    /**
     * Checks whether a piece ID is valid and still available.
     */
    private boolean isValidPiece(int id) {
        if (id < 0 || id > 15) return false;
        return localGame.getAvailablePieces().containsKey(id) && id != pieceInHand;
    }

    @Override
    public void onConnected() {
        System.out.println("✅ Connected to Server!");
    }

    @Override
    public void onNewGame(String p1, String p2) {
        localGame = new Game(1);
        Arrays.fill(visualBoard, -1);
        pieceInHand = -1;
        isGameActive = true;
        isMyTurn = false;
        waitingForServerEcho = false;

        if (!isAiMode)
            System.out.println("\n🟢 NEW GAME STARTED: " + p1 + " vs " + p2);

        // Always draw the board so Player 2 sees the grid immediately
        if (!isAiMode) drawBoard();

        if (p1.equals(username)) {
            isMyTurn = true;
            if (isAiMode) {
                makeAiMove();
            } else {
                System.out.println("👉 YOU START! Pick a piece to give.");
                System.out.println("✍️  Enter: <PIECE_TO_GIVE>");
            }
        } else {
            if (!isAiMode)
                System.out.println("⏳ Opponent's turn to pick the first piece...");
        }
    }

    /**
     * Handles both opponent moves and echoed confirmations of our own moves.
     */
    @Override
    public void onOpponentMove(int location, int piece) {
        if (localGame == null) return;

        try {
            int placedPiece = -1;

            if (location == -1) {
                localGame.doMove(new Move(piece));
            } else {
                placedPiece = localGame.getCurrentPieceID();
                localGame.doMove(new Move(piece, location));
                visualBoard[location] = placedPiece;
            }

            if (waitingForServerEcho) {
                // Our own move confirmed by the server
                waitingForServerEcho = false;
                isMyTurn = false;

                if (!isAiMode) {
                    drawBoard();
                    System.out.println("✅ Move accepted. Opponent is thinking...");
                }
            } else {
                // Opponent move received
                pieceInHand = piece;
                isMyTurn = true;

                if (!isAiMode) {
                    if (location == -1) {
                        System.out.println("🔻 Opponent gave you piece: [" + piece + "]");
                    } else {
                        System.out.println("🔻 Opponent placed [" + placedPiece +
                                                   "] at " + location + " and gave [" + piece + "]");
                    }
                    drawBoard();

                    if (!localGame.isGameOver()) {
                        System.out.println("✋ YOU HAVE: [" + pieceInHand + "]");
                        System.out.println("✍️  Enter: <LOCATION> <PIECE_TO_GIVE>");
                    }
                }

                if (isAiMode && isGameActive) {
                    makeAiMove();
                }
            }

        } catch (Exception e) {
            System.out.println("Sync error: " + e.getMessage());
        }
    }

    /**
     * Triggers AI move calculation and submission.
     */
    private void makeAiMove() {
        if (waitingForServerEcho) return;

        new Thread(() -> {
            try {
                Thread.sleep(100);
                Move move = aiClient.determineMove(localGame);
                if (move == null) return;

                client.sendMove(move.getLocation(), move.getNextPiece());
                waitingForServerEcho = true;
                System.out.println("🤖 Bot moved.");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onGameOver(String result, String winner) {
        isGameActive = false;
        isMyTurn = false;
        waitingForServerEcho = false;

        if (result.equals(Protocol.VICTORY)) {
            if (winner.equals(username))
                System.out.println("\n🏆 VICTORY! You won!");
            else
                System.out.println("\n💀 DEFEAT. Winner: " + winner);
        } else {
            System.out.println("\n🤝 DRAW GAME.");
        }

        localGame = null;

        if (isAiMode) {
            try { Thread.sleep(2000); } catch (Exception ignored) {}
            System.out.println("🤖 Bot re-queueing...");
            client.queue();
        } else {
            System.out.println("Game Over. Type Commands: queue | rank | quit ");
        }
    }

    @Override
    public void onError(String msg) {
        System.out.println("⚠️ SERVER ERROR: " + msg);
        waitingForServerEcho = false;
    }

    @Override
    public void onChat(String sender, String text) {
        if (!isAiMode)
            System.out.println("💬 " + sender + ": " + text);
    }

    /**
     * Draws the current board state and available pieces.
     */
    private void drawBoard() {
        System.out.println("\n      0     1     2     3");
        System.out.println("   ╔═════╦═════╦═════╦═════╗");

        for (int row = 0; row < 4; row++) {
            System.out.print("   ║");
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                int val = visualBoard[index];
                if (val == -1)
                    System.out.printf(" %3d ║", index);
                else
                    System.out.printf(" \u001B[1m[%2d]\u001B[0m║", val);
            }
            System.out.println();
            if (row < 3)
                System.out.println("   ╠═════╬═════╬═════╬═════╣");
            else
                System.out.println("   ╚═════╩═════╩═════╩═════╝");
        }

        if (localGame != null) {
            Set<Integer> available =
                    new TreeSet<>(localGame.getAvailablePieces().keySet());
            if (pieceInHand != -1) available.remove(pieceInHand);

            System.out.println("\n📦 AVAILABLE PIECES (Legend: Height, Color, Shape, Fill):");
            System.out.println("   Tall(T)/short(s) | Black(B)/White(W) | Square(Q)/Circle(O) | Solid(*)/Hollow(_)");
            System.out.println("   --------------------------------------------------------------------------");

            int count = 0;
            for (int id : available) {
                System.out.printf("   [%2d]: %-5s", id, getPieceStats(id));
                count++;
                if (count % 4 == 0) System.out.println();
            }
            System.out.println();
        }
    }

    /**
     * Returns a compact textual representation of a piece's attributes.
     */
    private String getPieceStats(int id) {
        boolean isTall   = (id & 8) != 0;
        boolean isBlack  = (id & 4) != 0;
        boolean isSquare = (id & 2) != 0;
        boolean isSolid  = (id & 1) != 0;

        String h = isTall   ? "T" : "s";
        String c = isBlack  ? "B" : "W";
        String s = isSquare ? "Q" : "O";
        String f = isSolid  ? "*" : "_";

        return String.format("%s%s%s%s", h, c, s, f);
    }
}
