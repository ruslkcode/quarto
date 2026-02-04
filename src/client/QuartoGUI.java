package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import protocol.Protocol;

import java.util.Optional;

public class QuartoGUI extends Application {

    private Stage primaryStage;
    private GridPane boardGrid;
    private FlowPane availablePiecesPane;
    private Label statusLabel;
    private Label turnLabel;
    private VBox currentPieceBox;
    private StackPane rootPane;

    private TextField ipField;
    private TextField portField;
    private TextField nameField;

    private QuartoClient client;
    private String myUsername;

    // --- GAME STATE ---
    private boolean isMyTurn = false;
    private boolean waitingForServerEcho = false;

    private int pieceToPlace = -1; // Piece we MUST place
    private int lastPlacedPiece = -1; // Piece we just placed (waiting for confirmation)
    private int pieceGivenToOpponent = -1; // Piece we selected for opponent

    private Integer[] localBoard = new Integer[16];
    private Button[] boardButtons = new Button[16];

    // --- STYLES (Modern Minimalist) ---
    private static final String BG_COLOR = "#212121"; // Dark Grey Background
    private static final String PANEL_COLOR = "#323232"; // Slightly lighter panels
    private static final String ACCENT_COLOR = "#00E5FF"; // Cyan/Teal Accent
    private static final String TEXT_PRIMARY = "#ECECEC"; // Off-white text
    private static final String TEXT_SECONDARY = "#B0BEC5"; // Muted text
    private static final String WIN_COLOR = "#00C853"; // Green
    private static final String LOSE_COLOR = "#D50000"; // Red

    // Piece Attribute Colors
    private static final Color PIECE_DARK = Color.web("#7C4DFF"); // Deep Purple
    private static final Color PIECE_LIGHT = Color.web("#FFAB00"); // Amber

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.client = new QuartoClient();
        primaryStage.setTitle("Quarto");

        showLoginScreen();
        primaryStage.show();
    }

    // ==========================================
    // SCREENS
    // ==========================================

    private void showLoginScreen() {
        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50));
        content.setStyle("-fx-background-color: " + BG_COLOR + ";");

        Label title = new Label("QUARTO");
        title.setFont(Font.font("Inter", FontWeight.BOLD, 50));
        title.setTextFill(Color.web(ACCENT_COLOR));

        Label subtitle = new Label("Modern Strategy Game");
        subtitle.setFont(Font.font("Inter", FontWeight.NORMAL, 16));
        subtitle.setTextFill(Color.web(TEXT_SECONDARY));

        VBox fields = new VBox(15);
        fields.setMaxWidth(300);
        nameField = createStyledTextField("Username", "Player1");
        ipField = createStyledTextField("Host IP", "localhost");
        portField = createStyledTextField("Port", "5432");
        fields.getChildren().addAll(nameField, ipField, portField);

        Button connectBtn = createStyledButton("CONNECT", true);
        connectBtn.setPrefWidth(300);
        connectBtn.setOnAction(e -> handleConnect());

        content.getChildren().addAll(title, subtitle, fields, connectBtn);
        primaryStage.setScene(new Scene(content, 450, 600));
    }

    private void showQueueScreen() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setStyle("-fx-progress-color: " + ACCENT_COLOR + ";");

        Label status = new Label("Connected to Lobby");
        status.setTextFill(Color.web(TEXT_PRIMARY));
        status.setFont(Font.font("Inter", 20));

        Button joinBtn = createStyledButton("FIND MATCH", true);
        joinBtn.setPrefWidth(200);
        joinBtn.setOnAction(e -> {
            joinBtn.setDisable(true);
            status.setText("Searching for opponent...");
            client.queue();
        });

        root.getChildren().addAll(status, spinner, joinBtn);
        primaryStage.setScene(new Scene(root, 450, 500));
    }

    private void showGameScreen() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: " + BG_COLOR + ";");

        // --- TOP BAR ---
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setStyle("-fx-background-color: " + PANEL_COLOR
                + "; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2);");

        Label gameTitle = new Label("QUARTO");
        gameTitle.setFont(Font.font("Inter", FontWeight.BOLD, 24));
        gameTitle.setTextFill(Color.web(ACCENT_COLOR));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        turnLabel = new Label("Waiting...");
        turnLabel.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 18));
        turnLabel.setTextFill(Color.web(TEXT_PRIMARY));

        statusLabel = new Label("Initializing...");
        statusLabel.setFont(Font.font("Inter", 14));
        statusLabel.setTextFill(Color.web(TEXT_SECONDARY));

        VBox statusBox = new VBox(2, turnLabel, statusLabel);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(gameTitle, spacer, statusBox);
        layout.setTop(topBar);

        // --- CENTER BOARD ---
        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(12);
        boardGrid.setVgap(12);
        boardGrid.setPadding(new Insets(20));

        for (int i = 0; i < 16; i++) {
            Button btn = new Button();
            btn.setPrefSize(90, 90);
            btn.setStyle(getButtonStyle(false));
            int finalI = i;
            btn.setOnAction(e -> handleBoardClick(finalI));
            boardButtons[i] = btn;
            boardGrid.add(btn, i % 4, i / 4);
        }
        layout.setCenter(boardGrid);

        // --- RIGHT SIDEBAR ---
        VBox sideBar = new VBox(25);
        sideBar.setPadding(new Insets(25));
        sideBar.setPrefWidth(320);
        sideBar.setStyle("-fx-background-color: " + PANEL_COLOR + ";");
        sideBar.setAlignment(Pos.TOP_CENTER);

        // Current Piece Section
        Label currentPieceTitle = new Label("PIECE TO PLACE");
        currentPieceTitle.setFont(Font.font("Inter", FontWeight.BOLD, 14));
        currentPieceTitle.setTextFill(Color.web(TEXT_SECONDARY));

        currentPieceBox = new VBox();
        currentPieceBox.setAlignment(Pos.CENTER);
        currentPieceBox.setPrefHeight(120);
        currentPieceBox.setStyle("-fx-background-color: " + BG_COLOR
                + "; -fx-background-radius: 10; -fx-border-color: #444; -fx-border-radius: 10;");
        currentPieceBox.getChildren().add(new Label("None"));

        // Available Pieces Section
        Label poolTitle = new Label("AVAILABLE PIECES");
        poolTitle.setFont(Font.font("Inter", FontWeight.BOLD, 14));
        poolTitle.setTextFill(Color.web(TEXT_SECONDARY));

        availablePiecesPane = new FlowPane();
        availablePiecesPane.setHgap(10);
        availablePiecesPane.setVgap(10);
        availablePiecesPane.setAlignment(Pos.CENTER);
        availablePiecesPane.setPrefWrapLength(280);

        sideBar.getChildren().addAll(currentPieceTitle, currentPieceBox, new Separator(), poolTitle,
                availablePiecesPane);
        layout.setRight(sideBar);

        rootPane = new StackPane(layout);
        primaryStage.setScene(new Scene(rootPane, 1000, 700));
    }

    // ==========================================
    // LOGIC & HANDLERS
    // ==========================================

    private void handleConnect() {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();
        myUsername = nameField.getText().trim();

        if (ip.isEmpty() || portStr.isEmpty() || myUsername.isEmpty()) {
            showErrorAlert("Warning", "All fields are required.");
            return;
        }

        new Thread(() -> {
            try {
                int port = Integer.parseInt(portStr);
                client.connect(ip, port, createGameListener());
                client.send("HELLO~GuiClient"); // Handshake if needed, or just login
                client.login(myUsername);

                Platform.runLater(this::showQueueScreen);
            } catch (NumberFormatException e) {
                Platform.runLater(() -> showErrorAlert("Error", "Invalid port number."));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(
                        () -> showErrorAlert("Connection Error", "Could not connect to server.\n" + ex.getMessage()));
            }
        }).start();
    }

    private void handleBoardClick(int index) {
        if (!isMyTurn || pieceToPlace == -1) {
            // Not placing phase
            return;
        }

        // Place on local board
        lastPlacedPiece = pieceToPlace;
        localBoard[index] = lastPlacedPiece;

        // Update UI immediately (optimistic)
        updateBoardButton(index, lastPlacedPiece);
        boardButtons[index].setDisable(true); // Prevent multi-click

        // Check Win
        if (checkLocalWin()) {
            waitingForServerEcho = true;
            statusLabel.setText("Verifying Victory...");
            client.sendMove(index, 16); // 16 signals "I win" usually in this protocol variant?
        } else {
            // Proceed to pick phase logic
            waitingForServerEcho = true; // Wait for server to confirm move wasn't illegal (though we check locally)

            statusLabel.setText("Phase 2: SELECT A PIECE FOR OPPONENT");
            turnLabel.setText("YOUR TURN: Pick Piece");
            pieceToPlace = -1; // Placed. Now empty hands.
            currentPieceBox.getChildren().clear(); // Hand empty

            enableAvailablePieces();
            disableBoard(); // Can't place anymore
        }
    }

    // We split the "Move" into 2 interactions: 1. Click Board (Place) -> 2. Click
    // Pool (Pick for Opponent)

    private void handlePieceSelect(int pieceId) {
        if (!isMyTurn)
            return;

        // If we are selecting a piece, checks:
        if (pieceToPlace != -1) {
            // We are holding a piece we need to Place. We can't pick from pool yet!
            return;
        }

        // If we are here, we must have just placed a piece (or it's the very first move
        // of the game?)
        // Calculate location of the piece we just placed.
        int location = findLastLocation(); // -1 if first move

        // Send to server
        client.sendMove(location, pieceId);

        // Update state
        pieceGivenToOpponent = pieceId;
        waitingForServerEcho = true; // Wait for server implementation to define turn switch

        // UI Feedback
        // Disable everything until server replies
        disableBoard();
        availablePiecesPane.setDisable(true);
        statusLabel.setText("Sending move...");
    }

    private QuartoClient.GameListener createGameListener() {
        return new QuartoClient.GameListener() {
            @Override
            public void onNewGame(String p1, String p2) {
                Platform.runLater(() -> {
                    resetGameState();
                    showGameScreen();

                    if (myUsername.equalsIgnoreCase(p1)) {
                        isMyTurn = true;
                        turnLabel.setText("YOUR TURN");
                        statusLabel.setText("Phase 1: Pick a piece for opponent");
                        refreshAvailablePieces();
                        enableAvailablePieces();
                        disableBoard(); // First move is just picking a piece
                    } else {
                        isMyTurn = false;
                        turnLabel.setText("OPPONENT'S TURN");
                        statusLabel.setText("Waiting for opponent...");
                        refreshAvailablePieces();
                        availablePiecesPane.setDisable(true);
                        disableBoard();
                    }
                });
            }

            @Override
            public void onOpponentMove(int location, int nextPieceId) {
                Platform.runLater(() -> {

                    if (waitingForServerEcho) {
                        // === CASE 1: Server confirmed MY move ===
                        // It means I successfully completed my turn.
                        waitingForServerEcho = false;
                        isMyTurn = false;

                        turnLabel.setText("OPPONENT'S TURN");
                        statusLabel.setText("Waiting for opponent...");

                        // Board is already updated locally from handleBoardClick
                        // Hand is empty.

                        disableBoard();
                        availablePiecesPane.setDisable(true);

                    } else {
                        // === CASE 2: Opponent made a move ===

                        // 1. Reflect Opponent's Placement (if valid)
                        // The opponent placed the piece that *I* (or previous player) gave them.
                        // We stored this in pieceGivenToOpponent when WE selected it.
                        // OR if we just joined, we rely on game state sync (not fully implemented in
                        // this simple client).
                        // Note: If I am player 2, first move I receive is (-1, PieceID).

                        if (location >= 0 && location < 16) {
                            if (pieceGivenToOpponent != -1) {
                                localBoard[location] = pieceGivenToOpponent;
                                updateBoardButton(location, pieceGivenToOpponent);
                                pieceGivenToOpponent = -1; // Consumed
                            }
                        }

                        // 2. Prepare for My Turn
                        // The opponent gave me 'nextPieceId'.

                        // If nextPieceId is 16 (Win claim), we might get Game Over soon, but logic
                        // stands.
                        if (nextPieceId >= 0 && nextPieceId < 16) {
                            isMyTurn = true;
                            pieceToPlace = nextPieceId;

                            turnLabel.setText("YOUR TURN");
                            statusLabel.setText("Phase 1: Place the piece on the board");

                            currentPieceBox.getChildren().setAll(createPieceShape(pieceToPlace));
                            enableEmptyBoardButtons();
                        } else {
                            // Valid piece not received (maybe game over code?)
                            statusLabel.setText("Checking Game Status...");
                        }

                        availablePiecesPane.setDisable(true); // Can't pick from pool until placed
                        refreshAvailablePieces();
                    }
                });
            }

            @Override
            public void onGameOver(String result, String winner) {
                Platform.runLater(() -> {
                    boolean iWon = myUsername.equalsIgnoreCase(winner.trim());
                    String color = iWon ? WIN_COLOR : LOSE_COLOR;
                    String message = iWon ? "VICTORY" : "DEFEAT";
                    if ("DRAW".equalsIgnoreCase(result)) {
                        message = "DRAW";
                        color = "#FFD700"; // Gold
                    }
                    showEndOverlay(message, "Winner: " + winner, color);
                });
            }

            @Override
            public void onConnected() {
            }

            @Override
            public void onError(String msg) {
                Platform.runLater(() -> showErrorAlert("Error", msg));
            }

            @Override
            public void onChat(String s, String t) {
            }
        };
    }

    // --- HELPER LOGIC ---

    private int findLastLocation() {
        // Find which button i just disabled / placed in localBoard that matches
        // `lastPlacedPiece`
        for (int i = 0; i < 16; i++) {
            if (localBoard[i] != null && localBoard[i] == lastPlacedPiece) {
                return i;
            }
        }
        return -1; // Should happen only on first turn if p1 picks piece first
    }

    private boolean checkLocalWin() {
        // Rows, Cols, Diagonals
        int[][] lines = {
                { 0, 1, 2, 3 }, { 4, 5, 6, 7 }, { 8, 9, 10, 11 }, { 12, 13, 14, 15 }, // Rows
                { 0, 4, 8, 12 }, { 1, 5, 9, 13 }, { 2, 6, 10, 14 }, { 3, 7, 11, 15 }, // Cols
                { 0, 5, 10, 15 }, { 3, 6, 9, 12 } // Diagonals
        };

        for (int[] line : lines) {
            if (hasCommonProperty(line))
                return true;
        }
        return false;
    }

    private boolean hasCommonProperty(int[] indices) {
        // Check if all are occupied
        for (int i : indices) {
            if (localBoard[i] == null)
                return false;
        }

        int p1 = localBoard[indices[0]];
        int p2 = localBoard[indices[1]];
        int p3 = localBoard[indices[2]];
        int p4 = localBoard[indices[3]];

        // 4 bits: 8, 4, 2, 1
        // (p1 & p2 & p3 & p4) != 0 => At least one bit is '1' in all 4
        // (~p1 & ~p2 & ... & 0xF) != 0 => At least one bit is '0' in all 4

        boolean commonOne = (p1 & p2 & p3 & p4) != 0;
        boolean commonZero = ((~p1 & 0xF) & (~p2 & 0xF) & (~p3 & 0xF) & (~p4 & 0xF)) != 0;

        return commonOne || commonZero;
    }

    private void refreshAvailablePieces() {
        availablePiecesPane.getChildren().clear();
        for (int i = 0; i < 16; i++) {
            boolean used = false;
            // Check board
            for (Integer p : localBoard) {
                if (p != null && p == i)
                    used = true;
            }
            // Check active hands (pieceToPlace is effectively 'used' if someone holding it)
            if (pieceToPlace == i && pieceToPlace != -1)
                used = true;

            if (used)
                continue;

            final int pId = i; // capture
            Button b = new Button();
            b.setGraphic(createPieceShape(i, 40));
            b.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            b.setOnAction(e -> handlePieceSelect(pId));

            // Hover effect
            b.setOnMouseEntered(
                    e -> b.setStyle("-fx-background-color: #383838; -fx-cursor: hand; -fx-background-radius: 5;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent;"));

            availablePiecesPane.getChildren().add(b);
        }
    }

    // ==========================================
    // UI HELPERS
    // ==========================================

    private void resetGameState() {
        waitingForServerEcho = false;
        pieceToPlace = -1;
        lastPlacedPiece = -1;
        pieceGivenToOpponent = -1;
        localBoard = new Integer[16];
        boardButtons = new Button[16]; // Will be recreated in showGameScreen
    }

    private void updateBoardButton(int index, int pieceId) {
        Button btn = boardButtons[index];
        btn.setGraphic(createPieceShape(pieceId, 60));
        btn.setDisable(true);
        // Force opacity to 1.0 to prevent graying out
        btn.setOpacity(1.0);
        btn.setStyle("-fx-background-color: #2A2A2A; -fx-opacity: 1.0; -fx-border-color: #444;");
    }

    private void enableEmptyBoardButtons() {
        for (int i = 0; i < 16; i++) {
            if (localBoard[i] == null) {
                boardButtons[i].setDisable(false);
            }
        }
    }

    private void disableBoard() {
        for (Button b : boardButtons) {
            if (b != null)
                b.setDisable(true);
        }
    }

    private void enableAvailablePieces() {
        availablePiecesPane.setDisable(false);
    }

    /**
     * Creates the visual representation of a piece.
     * ID is 4 bits: HOLLOW (8), SQUARE (4), TALL (2), DARK (1)
     */
    private StackPane createPieceShape(int id) {
        return createPieceShape(id, 50);
    }

    private StackPane createPieceShape(int id, double baseSize) {
        boolean hollow = (id & 8) != 0;
        boolean square = (id & 4) != 0;
        boolean tall = (id & 2) != 0;
        boolean dark = (id & 1) != 0;

        Shape shape;
        double size = tall ? baseSize : baseSize * 0.65;

        if (square) {
            Rectangle r = new Rectangle(size, size);
            r.setArcWidth(10);
            r.setArcHeight(10); // Soft corners
            shape = r;
        } else {
            shape = new Circle(size / 2);
        }

        Color c = dark ? PIECE_DARK : PIECE_LIGHT;

        shape.setFill(hollow ? Color.TRANSPARENT : c);
        shape.setStroke(c);
        shape.setStrokeWidth(hollow ? 4 : 0);

        // Add "Height" indicator or distinct look for tall/short if size not obvious?
        // Size handles it, but let's add a subtle shadow for tall pieces to make them
        // pop properly
        if (tall) {
            shape.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.5)));
        }

        StackPane s = new StackPane(shape);
        // If hollow, maybe add a dot in center if it's too hard to see?
        // No, stroke is thick enough.

        return s;
    }

    private Button createStyledButton(String text, boolean primary) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Inter", FontWeight.BOLD, 14));
        btn.setPrefHeight(45);
        if (primary) {
            btn.setStyle("-fx-background-color: " + ACCENT_COLOR
                    + "; -fx-text-fill: #000; -fx-background-radius: 8; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ACCENT_COLOR + "; -fx-border-color: "
                    + ACCENT_COLOR + "; -fx-border-radius: 8; -fx-cursor: hand;");
        }
        return btn;
    }

    private String getButtonStyle(boolean active) {
        return "-fx-background-color: #2F2F2F; -fx-background-radius: 8; -fx-border-color: #3E3E3E; -fx-border-radius: 8; -fx-effect: null;";
    }

    private TextField createStyledTextField(String label, String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        t.setText(prompt); // Pre-fill for convenience
        t.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 10;");
        return t;
    }

    private void showEndOverlay(String title, String sub, String colorHex) {
        VBox overlay = new VBox(25);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.85);");

        Label l1 = new Label(title);
        l1.setFont(Font.font("Inter", FontWeight.BOLD, 60));
        l1.setTextFill(Color.web(colorHex));
        l1.setEffect(new DropShadow(20, Color.web(colorHex)));

        Label l2 = new Label(sub);
        l2.setFont(Font.font("Inter", 24));
        l2.setTextFill(Color.WHITE);

        Button btn = createStyledButton("PLAY AGAIN", true);
        btn.setPrefWidth(200);
        btn.setOnAction(e -> {
            client.queue();
            showQueueScreen();
        });

        overlay.getChildren().addAll(l1, l2, btn);
        rootPane.getChildren().add(overlay);
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}