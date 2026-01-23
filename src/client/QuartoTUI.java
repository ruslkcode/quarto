package client;

import gameLogic.Game;
import gameLogic.Move;
import protocol.Protocol;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Text User Interface (Final Fix).
 * Solves:
 * 1. Server Echo interpreting own moves as opponent's.
 * 2. Board visibility on first turn.
 * 3. AI infinite loop / spam.
 */
public class QuartoTUI implements QuartoClient.GameListener {

    private QuartoClient client;
    private AbstractClient aiClient;
    private Game localGame;
    private String username;
    private Scanner scanner;
    private HumanClient humanClient;


    // Game State
    private int[] visualBoard = new int[16];
    private int pieceInHand = -1; // Piece we must place
    private boolean isAiMode = false;
    private boolean isGameActive = false;

    // TURN LOGIC (CRITICAL FIXES)
    private volatile boolean isMyTurn = false;
    private volatile boolean waitingForServerEcho = false;

    public static void main(String[] args) {
        new QuartoTUI().start();
    }

    public void start() {
        scanner = new Scanner(System.in);
        client = new QuartoClient();
        Arrays.fill(visualBoard, -1);

        System.out.print("Enter Username: ");
        username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "Player" + (int)(Math.random() * 100);
        }

        System.out.println("Select Player Type:");
        System.out.println("1. Human");
        System.out.println("2. AI");
        String type = scanner.nextLine().trim();

        if (type.equals("2")) {
            isAiMode = true;
            setupAI();
        } else {
            isAiMode = false;
            humanClient = new HumanClient(username, scanner);
        }

        System.out.print("Server port (Enter for 5432): ");
        int port = 5432;
        try {
            String p = scanner.nextLine().trim();
            if (!p.isEmpty()) port = Integer.parseInt(p);
        } catch (Exception ignored) {}

        try {
            client.connect("localhost", port, this);
            client.login(username);

            if (!isAiMode) {
                humanCommandLoop();   // человек сам пишет queue
            } else {
                client.queue();       // AI сразу в очередь
                while (true) Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }



    private void setupAI() {
        System.out.println("Select AI Strategy:");
        System.out.println("1. Naive (Random)");
        System.out.println("2. Smart (Minimax/Heuristic)");
        System.out.print("> ");
        String choice = scanner.nextLine();

        BotStrategy strategy;
        if (choice.equals("2")) strategy = new SmartStrategy();
        else strategy = new NaiveStrategy();

        // Give AI 1 second delay to feel like a real player
        aiClient = new AIClient(username, strategy, 1000);
        System.out.println("✅ AI Configured: " + strategy.getName());
    }

    // ==========================================
    // HUMAN INPUT LOOP
    // ==========================================

    private void makeHumanMove() {
        new Thread(() -> {
            try {
                Move move = humanClient.determineMove(localGame);
                if (move == null) return;

                waitingForServerEcho = true; // ONLY after move is chosen
                client.sendMove(move.getLocation(), move.getNextPiece());

            } catch (Exception e) {
                System.out.println("Human move error: " + e.getMessage());
                waitingForServerEcho = false;
            }
        }).start();
    }



    private void humanCommandLoop() {
        while (true) {
            String cmd = scanner.nextLine().trim().toLowerCase();

            switch (cmd) {
                case "queue":
                    client.queue();
                    break;
                case "list":
                    client.listPlayers();
                    break;
                case "quit":
                    client.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Commands: queue | list | quit");
            }
        }
    }




    // NETWORK EVENTS

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
        waitingForServerEcho = false;
        isMyTurn = false;

        drawBoard();

        if (p1.equals(username)) {
            isMyTurn = true;
            triggerMyMove();
        } else {
            System.out.println("Opponent starts.");
        }
    }


    // This handles BOTH Opponent moves AND our own echoed moves
    @Override
    public void onOpponentMove(int location, int piece) {
        if (localGame == null) return;

        int placed = -1;

        if (location == -1) {
            localGame.doMove(new Move(piece));
        } else {
            placed = localGame.getCurrentPieceID();
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

        if (isGameActive) {
            triggerMyMove();
        }
    }


    private void makeAiMove() {
        if (waitingForServerEcho) return;

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Move move = aiClient.determineMove(localGame);
                if (move == null) return;

                // Send and set wait flag
                client.sendMove(move.getLocation(), move.getNextPiece());
                waitingForServerEcho = true;
                System.out.println("🤖 Bot moved.");

            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @Override
    public  void onGameOver(String result, String winner) {
        isGameActive = false;
        isMyTurn = false;
        waitingForServerEcho = false;

        if (result.equals(Protocol.VICTORY)) {
            if (winner.equals(username)) System.out.println("\n🏆 VICTORY! You won!");
            else System.out.println("\n💀 DEFEAT. Winner: " + winner);
        } else {
            System.out.println("\n🤝 DRAW GAME.");
        }

        localGame = null;

        if (isAiMode) {
            try { Thread.sleep(2000); } catch (Exception e) {}
            System.out.println("🤖 Bot re-queueing...");
            client.queue();
        } else {
            System.out.println("Game Over. Type 'queue' to play again.");
        }
    }

    @Override
    public void onError(String msg) {
        System.out.println("⚠️ SERVER ERROR: " + msg);
        // If error happens during move, unlock
        waitingForServerEcho = false;
    }

    @Override
    public void onChat(String sender, String text) {
        if (!isAiMode) System.out.println("💬 " + sender + ": " + text);
    }

    private void drawBoard() {
        System.out.println("\n      0     1     2     3");
        System.out.println("   ╔═════╦═════╦═════╦═════╗");
        for (int row = 0; row < 4; row++) {
            System.out.print("   ║");
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                int val = visualBoard[index];
                if (val == -1) System.out.printf(" %3d ║", index);
                else System.out.printf(" \u001B[1m[%2d]\u001B[0m║", val);
            }
            System.out.println();
            if (row < 3) System.out.println("   ╠═════╬═════╬═════╬═════╣");
            else System.out.println("   ╚═════╩═════╩═════╩═════╝");
        }

        if (localGame != null) {
            Set<Integer> available = new TreeSet<>(localGame.getAvailablePieces().keySet());
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
            System.out.println("\n");
        }
    }

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

    private void triggerMyMove() {
        if (!isGameActive || !isMyTurn || waitingForServerEcho) return;

        if (isAiMode) {
            makeAiMove();
        } else {
            makeHumanMove();
        }
    }

}