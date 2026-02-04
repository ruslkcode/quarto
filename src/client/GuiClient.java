package client;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GuiClient extends Application {

    private Stage primaryStage;

    // UI Layout containers
    private StackPane rootContainer; // Holds Background + Content
    private Canvas particleCanvas;
    private StackPane contentLayer; // Holds the actual current screen

    // Game Components
    private GridPane boardGrid;
    private FlowPane availablePiecesPane;
    private Label statusLabel;
    private VBox currentPieceBox;
    private Button claimWinBtn;

    // Inputs
    private TextField ipField;
    private TextField portField;
    private TextField nameField;

    // Logic / Networking
    private QuartoClient client;
    private String myUsername;
    private String serverUsername;

    // Game State
    private boolean isMyTurn = false;
    private boolean waitingForServerEcho = false;
    private int pieceToPlace = -1;
    private int placedLocation = -1;
    private int pieceGivenToOpponent = -1;

    private Integer[] localBoard = new Integer[16];
    private List<Integer> usedPieces = new ArrayList<>();
    private Button[] boardButtons = new Button[16];

    // --- VISUAL CONSTANTS (NEON THEME) ---
    private static final Color BG_COLOR_1 = Color.web("#0f0f13");
    private static final Color BG_COLOR_2 = Color.web("#1a1a2e");

    private static final String ACCENT_CYAN = "#00f3ff"; // Neon Cyan
    private static final String ACCENT_PURPLE = "#bc13fe"; // Neon Purple
    private static final String ACCENT_GOLD = "#ffe600"; // Neon Gold
    private static final String ACCENT_RED = "#ff0055"; // Neon Red
    private static final String ACCENT_GREEN = "#00ff99"; // Neon Green

    // Fonts
    private static final String FONT_FAMILY = "Segoe UI"; // Fallback to standard, reliable font

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.client = new QuartoClient();
        primaryStage.setTitle("Q U A R T O  |  P R O J E C T");

        // 1. Initialize Root Layout
        rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #050505, #101015);");

        // 2. Initialize Particle System
        particleCanvas = new Canvas(1200, 800);
        particleCanvas.widthProperty().bind(rootContainer.widthProperty());
        particleCanvas.heightProperty().bind(rootContainer.heightProperty());

        // 3. Content Layer
        contentLayer = new StackPane();
        contentLayer.setPadding(new Insets(20));

        // 4. Combine
        rootContainer.getChildren().addAll(particleCanvas, contentLayer);

        // 5. Start Background Animation
        startParticleAnimation();

        // 6. Show Initial Screen
        showLoginScreen();

        Scene scene = new Scene(rootContainer, 1100, 750);

        // Add a global stylesheet or inline styles if preferred. We'll use inline for
        // self-containment.
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ==========================================
    // SCENE MANAGEMENT
    // ==========================================

    private void transitionTo(Region newContent) {
        FadeTransition ftOut = new FadeTransition(Duration.millis(300), contentLayer);
        ftOut.setFromValue(1.0);
        ftOut.setToValue(0.0);
        ftOut.setOnFinished(e -> {
            contentLayer.getChildren().clear();
            contentLayer.getChildren().add(newContent);
            FadeTransition ftIn = new FadeTransition(Duration.millis(300), contentLayer);
            ftIn.setFromValue(0.0);
            ftIn.setToValue(1.0);
            ftIn.play();
        });
        ftOut.play();
    }

    // ==========================================
    // 1. LOGIN SCREEN
    // ==========================================
    private void showLoginScreen() {
        VBox card = createGlassCard(450, 550);

        Label logo = new Label("QUARTO");
        logo.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 64));
        logo.setTextFill(Color.web(ACCENT_CYAN));
        logo.setEffect(createNeonEffect(Color.web(ACCENT_CYAN)));

        Label sub = new Label("CYBERPUNK EDITION");
        sub.setFont(Font.font(FONT_FAMILY, FontWeight.LIGHT, 16));
        sub.setTextFill(Color.WHITE);
        sub.setOpacity(0.7);

        int rnd = new Random().nextInt(1000);
        nameField = createStyledTextField("Player" + rnd);
        ipField = createStyledTextField("localhost");
        portField = createStyledTextField("5432");

        Button connectBtn = createNeonButton("CONNECT SYSTEM", Color.web(ACCENT_CYAN));
        connectBtn.setPrefWidth(280);
        connectBtn.setOnAction(e -> handleConnect());

        VBox fields = new VBox(15, nameField, ipField, portField);
        fields.setAlignment(Pos.CENTER);
        fields.setMaxWidth(300);

        card.getChildren().addAll(logo, sub, new Region() {
            {
                setMinHeight(30);
            }
        }, fields, new Region() {
            {
                setMinHeight(30);
            }
        }, connectBtn);

        // Directly set context for first load
        contentLayer.getChildren().clear();
        contentLayer.getChildren().add(card);
    }

    private void handleConnect() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (Exception e) {
            return;
        }
        if (name.isEmpty())
            return;

        this.myUsername = name;
        this.serverUsername = name;

        // Show Loading
        VBox loadingCard = createGlassCard(300, 200);
        ProgressIndicator pi = new ProgressIndicator();
        pi.setStyle("-fx-progress-color: " + ACCENT_CYAN + ";");
        Label l = new Label("ESTABLISHING UPLINK...");
        l.setTextFill(Color.WHITE);
        loadingCard.getChildren().addAll(pi, l);

        transitionTo(loadingCard);

        new Thread(() -> {
            try {
                client.connect(ip, port, createGameListener());
                client.send("HELLO~GuiClient");
                client.login(name);
                Platform.runLater(this::showQueueScreen);
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Connection failed: " + ex.getMessage());
                    a.show();
                    showLoginScreen();
                });
            }
        }).start();
    }

    // ==========================================
    // 2. LOBBY
    // ==========================================
    private void showQueueScreen() {
        VBox card = createGlassCard(600, 400);

        Label title = new Label("LOBBY");
        title.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 42));
        title.setTextFill(Color.WHITE);

        Label userLbl = new Label("OPERATOR: " + myUsername.toUpperCase());
        userLbl.setTextFill(Color.web(ACCENT_CYAN));
        userLbl.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));

        Button findMatchBtn = createNeonButton("INITIATE MATCHMAKING", Color.web(ACCENT_PURPLE));
        findMatchBtn.setPrefSize(250, 60);

        VBox statusBox = new VBox(10);
        statusBox.setAlignment(Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setVisible(false);
        spinner.setStyle("-fx-progress-color: " + ACCENT_PURPLE + ";");

        Label statusLbl = new Label("SYSTEM IDLE");
        statusLbl.setTextFill(Color.GRAY);

        findMatchBtn.setOnAction(e -> {
            client.queue();
            findMatchBtn.setDisable(true);
            spinner.setVisible(true);
            statusLbl.setText("SCANNING FOR OPPONENTS...");
            statusLbl.setTextFill(Color.web(ACCENT_PURPLE));
            statusLbl.setEffect(createNeonEffect(Color.web(ACCENT_PURPLE)));
        });

        statusBox.getChildren().addAll(spinner, statusLbl);

        card.getChildren().addAll(title, userLbl, new Separator(), new Region() {
            {
                setMinHeight(40);
            }
        }, findMatchBtn, new Region() {
            {
                setMinHeight(20);
            }
        }, statusBox);
        transitionTo(card);
    }

    // ==========================================
    // 3. GAME SCREEN
    // ==========================================
    private void showGameScreen() {
        BorderPane layout = new BorderPane();

        // Top Bar
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));

        VBox glassTop = createGlassContainer(1000, 60);
        glassTop.getChildren().add(topBar);

        statusLabel = new Label("GAME STARTED");
        statusLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 20));
        statusLabel.setTextFill(Color.WHITE);

        topBar.getChildren().add(statusLabel);
        layout.setTop(glassTop);
        BorderPane.setMargin(glassTop, new Insets(0, 0, 20, 0));

        // Center Board
        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(12);
        boardGrid.setVgap(12);

        // Create 4x4 Grid
        for (int i = 0; i < 16; i++) {
            Button btn = createBoardSlot(i);
            boardButtons[i] = btn;
            boardGrid.add(btn, i % 4, i / 4);
        }

        VBox boardContainer = createGlassContainer(500, 500);
        boardContainer.setAlignment(Pos.CENTER);
        boardContainer.getChildren().add(boardGrid);

        layout.setCenter(boardContainer);

        // Right Panel (Controls)
        VBox rightPanel = new VBox(20);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPadding(new Insets(20));

        VBox glassRight = createGlassContainer(300, 600);
        glassRight.setAlignment(Pos.TOP_CENTER);
        glassRight.setPadding(new Insets(20));

        currentPieceBox = new VBox(15);
        currentPieceBox.setAlignment(Pos.CENTER);
        currentPieceBox.setMinHeight(120);
        currentPieceBox.setStyle("-fx-border-color: #333; -fx-border-radius: 10; -fx-border-style: dashed;");
        Label lblCur = new Label("INCOMING DATA");
        lblCur.setTextFill(Color.GRAY);
        currentPieceBox.getChildren().add(lblCur);

        claimWinBtn = createNeonButton("CLAIM VICTORY", Color.web(ACCENT_GOLD));
        claimWinBtn.setDisable(true);
        claimWinBtn.setOnAction(e -> handleClaimWin());

        Label lblPool = new Label("AVAILABLE ASSETS");
        lblPool.setTextFill(Color.WHITE);
        lblPool.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));

        availablePiecesPane = new FlowPane();
        availablePiecesPane.setAlignment(Pos.CENTER);
        availablePiecesPane.setHgap(10);
        availablePiecesPane.setVgap(10);
        availablePiecesPane.setPrefWrapLength(280);

        glassRight.getChildren().addAll(currentPieceBox, new Region() {
            {
                setMinHeight(10);
            }
        }, claimWinBtn, new Separator(), lblPool, availablePiecesPane);

        layout.setRight(glassRight);
        BorderPane.setMargin(glassRight, new Insets(0, 0, 0, 20));

        transitionTo(layout);
    }

    private Button createBoardSlot(int index) {
        Button btn = new Button();
        btn.setPrefSize(90, 90);
        btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10; -fx-cursor: hand;");

        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) {
                btn.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 10; -fx-border-color: "
                                + ACCENT_CYAN + "; -fx-border-radius: 10;");
                btn.setEffect(new Glow(0.5));
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) {
                btn.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10;");
                btn.setEffect(null);
            }
        });

        btn.setOnAction(e -> handleBoardClick(index));
        return btn;
    }

    private void showGameOverScreen(String result, String winner) {
        VBox overlay = createGlassCard(500, 350);
        overlay.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.85); -fx-background-radius: 20; -fx-border-color: white; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 50, 0, 0, 0);");

        Label resLbl = new Label();
        resLbl.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 48));

        String w = winner.trim();
        String me = serverUsername != null ? serverUsername.trim() : myUsername.trim();
        boolean iWon = w.equalsIgnoreCase(me) || w.contains(me) || me.contains(w);

        if (iWon) {
            resLbl.setText("VICTORY");
            resLbl.setTextFill(Color.web(ACCENT_GREEN));
            resLbl.setEffect(createNeonEffect(Color.web(ACCENT_GREEN)));
        } else if (result.contains("DRAW")) {
            resLbl.setText("DRAW SYSTEM");
            resLbl.setTextFill(Color.WHITE);
        } else {
            resLbl.setText("CRITICAL FAILURE");
            resLbl.setTextFill(Color.web(ACCENT_RED));
            resLbl.setEffect(createNeonEffect(Color.web(ACCENT_RED)));
        }

        Label winLbl = new Label("WINNER: " + winner.toUpperCase());
        winLbl.setFont(Font.font("Consolas", 18));
        winLbl.setTextFill(Color.LIGHTGRAY);

        Button playAgainBtn = createNeonButton("REBOOT SYSTEM", Color.web(ACCENT_CYAN));
        playAgainBtn.setPrefSize(220, 50);

        playAgainBtn.setOnAction(e -> {
            client.queue();
            showQueueScreen();
        });

        overlay.getChildren().addAll(resLbl, winLbl, new Region() {
            {
                setMinHeight(30);
            }
        }, playAgainBtn);

        // Add as overlay on top of game
        contentLayer.getChildren().add(overlay);
        FadeTransition ft = new FadeTransition(Duration.millis(500), overlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ==========================================
    // LOGIC CONNECTORS
    // ==========================================
    // Most logic remains same, just calling new UI update methods

    private QuartoClient.GameListener createGameListener() {
        return new QuartoClient.GameListener() {
            @Override
            public void onConnected() {
            }

            @Override
            public void onNewGame(String p1, String p2) {
                Platform.runLater(() -> {
                    resetGameState();
                    showGameScreen();

                    if (p1.contains(myUsername))
                        serverUsername = p1;
                    else if (p2.contains(myUsername))
                        serverUsername = p2;
                    else
                        serverUsername = myUsername;

                    if (serverUsername.equals(p1)) {
                        isMyTurn = true;
                        pieceToPlace = -1;
                        updateStatus("YOUR TURN: SELECT PAYLOAD FOR OPPONENT", Color.web(ACCENT_CYAN));
                        refreshAvailablePieces();
                        disableBoard();
                        claimWinBtn.setDisable(true);
                    } else {
                        isMyTurn = false;
                        updateStatus("WAITING FOR OPPONENT TRANSMISSION...", Color.GRAY);
                        refreshAvailablePieces();
                        availablePiecesPane.setDisable(true);
                        disableBoard();
                        claimWinBtn.setDisable(true);
                    }
                });
            }

            @Override
            public void onOpponentMove(int location, int nextPieceId) {
                Platform.runLater(() -> {
                    if (waitingForServerEcho) {
                        waitingForServerEcho = false;
                        return;
                    }

                    if (location >= 0 && location < 16) {
                        int piecePlaced = (pieceGivenToOpponent != -1) ? pieceGivenToOpponent : -1;
                        if (piecePlaced != -1) {
                            updateBoard(location, piecePlaced);
                        }
                    }

                    isMyTurn = true;
                    pieceToPlace = nextPieceId;
                    placedLocation = -1;

                    refreshAvailablePieces();

                    updateStatus("YOUR TURN: DEPLOY PAYLOAD TO SECTOR", Color.web(ACCENT_CYAN));

                    currentPieceBox.getChildren().clear();
                    Label lbl = new Label("DEPLOY THIS ASSET:");
                    lbl.setTextFill(Color.WHITE);
                    lbl.setFont(Font.font("Consolas", 12));
                    currentPieceBox.getChildren().addAll(lbl, createPieceVisual(pieceToPlace, 60));

                    enableEmptyBoard();
                    availablePiecesPane.setDisable(true);
                    claimWinBtn.setDisable(true);
                });
            }

            @Override
            public void onGameOver(String result, String winner) {
                Platform.runLater(() -> showGameOverScreen(result, winner));
            }

            @Override
            public void onError(String msg) {
                Platform.runLater(() -> {
                    statusLabel.setText("ERROR: " + msg);
                    statusLabel.setTextFill(Color.web(ACCENT_RED));
                    if (isMyTurn) {
                        if (placedLocation != -1) {
                            availablePiecesPane.setDisable(false);
                            claimWinBtn.setDisable(false);
                        } else {
                            enableEmptyBoard();
                        }
                        waitingForServerEcho = false;
                    }
                });
            }

            @Override
            public void onChat(String s, String t) {
            }
        };
    }

    private void handleBoardClick(int index) {
        if (!isMyTurn)
            return;

        if (pieceToPlace != -1) {
            placedLocation = index;
            updateBoard(index, pieceToPlace);
            pieceToPlace = -1;
            statusLabel.setText("SELECT PAYLOAD FOR OPPONENT");
            statusLabel.setTextFill(Color.web(ACCENT_GOLD));

            availablePiecesPane.setDisable(false);
            claimWinBtn.setDisable(false);

        } else {
            statusLabel.setText("NEGATIVE. DEPLOY ASSIGNED PAYLOAD FIRST.");
            statusLabel.setTextFill(Color.web(ACCENT_RED));
        }
    }

    private void handlePieceSelect(int pieceId) {
        if (!isMyTurn || usedPieces.contains(pieceId))
            return;

        if (placedLocation != -1) {
            client.sendMove(placedLocation, pieceId);
            pieceGivenToOpponent = pieceId;
            usedPieces.add(pieceId);

            isMyTurn = false;
            placedLocation = -1;
            statusLabel.setText("DATA SENT. WAITING...");
            statusLabel.setTextFill(Color.GRAY);

            disableBoard();
            availablePiecesPane.setDisable(true);
            claimWinBtn.setDisable(true);
        }
    }

    private void handleClaimWin() {
        if (placedLocation == -1)
            return;
        waitingForServerEcho = true;
        client.sendMove(placedLocation, 16);
        updateStatus("BROADCASTING VICTORY...", Color.web(ACCENT_GOLD));
        disableBoard();
        availablePiecesPane.setDisable(true);
        claimWinBtn.setDisable(true);
    }

    private void refreshAvailablePieces() {
        availablePiecesPane.getChildren().clear();
        for (int i = 0; i < 16; i++) {
            boolean isOnBoard = false;
            for (Integer p : localBoard) {
                if (p != null && p == i)
                    isOnBoard = true;
            }

            if (isOnBoard || usedPieces.contains(i) || i == pieceToPlace || i == pieceGivenToOpponent)
                continue;

            final int pId = i;
            Button btn = new Button();
            btn.setGraphic(createPieceVisual(i, 40));
            btn.setStyle("-fx-background-color: transparent; -fx-padding: 5;");

            btn.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
                st.setToX(1.1);
                st.setToY(1.1);
                st.play();
                btn.setEffect(new Glow(0.8));
            });
            btn.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
                btn.setEffect(null);
            });

            btn.setOnAction(e -> handlePieceSelect(pId));
            availablePiecesPane.getChildren().add(btn);
        }
    }

    private void updateBoard(int index, int pieceId) {
        if (index < 0 || index >= 16)
            return;
        localBoard[index] = pieceId;
        Button btn = boardButtons[index];
        if (btn != null) {
            btn.setGraphic(createPieceVisual(pieceId, 60));
            btn.setDisable(true);
            btn.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 10; -fx-border-color: "
                    + ACCENT_CYAN + "; -fx-opacity: 1;");

            // Placement Animation
            FadeTransition ft = new FadeTransition(Duration.millis(300), btn);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    private void resetGameState() {
        waitingForServerEcho = false;
        pieceGivenToOpponent = -1;
        placedLocation = -1;
        pieceToPlace = -1;
        usedPieces.clear();
        localBoard = new Integer[16];
    }

    private void updateStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(color);
        if (!color.equals(Color.GRAY)) {
            statusLabel.setEffect(createNeonEffect(color));
        } else {
            statusLabel.setEffect(null);
        }
    }

    private void disableBoard() {
        for (Button b : boardButtons)
            if (b != null)
                b.setDisable(true);
    }

    private void enableEmptyBoard() {
        for (Button b : boardButtons) {
            if (b != null && b.getGraphic() == null) {
                b.setDisable(false);
                b.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.2); -fx-cursor: hand;");
            }
        }
    }

    // ==========================================
    // UTILITIES & VFX
    // ==========================================

    private VBox createGlassCard(double w, double h) {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setMaxSize(w, h);
        box.setPadding(new Insets(30));
        // Glassmorphism style
        box.setStyle("-fx-background-color: rgba(30, 30, 40, 0.7); " +
                "-fx-background-radius: 20; " +
                "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                "-fx-border-radius: 20; " +
                "-fx-border-width: 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 20, 0, 0, 10);");
        return box;
    }

    private VBox createGlassContainer(double w, double h) {
        VBox box = new VBox();
        box.setPrefSize(w, h);
        box.setStyle("-fx-background-color: rgba(20, 20, 30, 0.6); " +
                "-fx-background-radius: 15; " +
                "-fx-border-color: rgba(255, 255, 255, 0.05); " +
                "-fx-border-radius: 15;");
        return box;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); " +
                "-fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; " +
                "-fx-background-radius: 5; " +
                "-fx-border-color: #444; " +
                "-fx-border-radius: 5; " +
                "-fx-padding: 10;");
        tf.setPrefHeight(45);
        tf.setMaxWidth(300);

        tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal)
                tf.setStyle(tf.getStyle().replace("-fx-border-color: #444;", "-fx-border-color: " + ACCENT_CYAN + ";"));
            else
                tf.setStyle(tf.getStyle().replace("-fx-border-color: " + ACCENT_CYAN + ";", "-fx-border-color: #444;"));
        });

        return tf;
    }

    private Button createNeonButton(String text, Color color) {
        Button btn = new Button(text);
        String hex = toHexString(color);

        String normal = "-fx-background-color: transparent; -fx-text-fill: " + hex
                + "; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 30; -fx-border-color: " + hex
                + "; -fx-border-radius: 30; -fx-border-width: 2; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + hex
                + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 30; -fx-border-color: "
                + hex
                + "; -fx-border-radius: 30; -fx-border-width: 2; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, "
                + hex + ", 15, 0.3, 0, 0);";

        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
        return btn;
    }

    private Effect createNeonEffect(Color color) {
        DropShadow glow = new DropShadow();
        glow.setColor(color);
        glow.setRadius(20);
        glow.setSpread(0.4);
        return glow;
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    // --- VISUAL PIECE GENERATOR ---
    private StackPane createPieceVisual(int id, double size) {
        boolean isHollow = (id & 1) != 0;
        boolean isDark = (id & 2) == 0;
        boolean isRound = (id & 4) == 0;
        boolean isBig = (id & 8) == 0; // if 0, actually Big

        // Interpret traits for visual style:
        // Round vs Rect -> Shape
        // Dark vs Light -> Color (Purple vs Orange)
        // Hollow vs Solid -> Fill vs Stroke only
        // Big vs Small -> Size modifier

        Shape shape;
        double displaySize = isBig ? size : size * 0.6;

        if (isRound) {
            shape = new Circle(displaySize / 2);
        } else {
            shape = new Rectangle(displaySize, displaySize);
        }

        Color mainColor = isDark ? Color.web(ACCENT_PURPLE) : Color.web(ACCENT_GOLD);

        if (isHollow) {
            shape.setFill(Color.TRANSPARENT);
            shape.setStroke(mainColor);
            shape.setStrokeWidth(3);
        } else {
            shape.setFill(mainColor);
            shape.setStroke(null);
        }

        shape.setEffect(new DropShadow(15, mainColor));

        StackPane sp = new StackPane(shape);
        sp.setPrefSize(size, size);
        return sp;
    }

    // ==========================================
    // PARTICLE SYSTEM
    // ==========================================

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private void startParticleAnimation() {
        // Initialize particles
        for (int i = 0; i < 70; i++) {
            particles.add(new Particle(rootContainer.getWidth(), rootContainer.getHeight()));
        }

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderParticles();
            }
        };
        timer.start();
    }

    private void renderParticles() {
        GraphicsContext gc = particleCanvas.getGraphicsContext2D();
        double w = particleCanvas.getWidth();
        double h = particleCanvas.getHeight();

        // Clear with fade to create trails? No, just clear for clean 60fps
        gc.clearRect(0, 0, w, h);

        // Update and draw
        gc.setGlobalAlpha(0.6);
        for (Particle p : particles) {
            p.update(w, h);
            gc.setFill(p.color);
            gc.fillOval(p.x, p.y, p.size, p.size);
        }

        // Draw connecting lines
        gc.setLineWidth(0.5);
        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                double dist = distance(p1, p2);
                if (dist < 100) {
                    gc.setStroke(Color.rgb(200, 200, 255, 1.0 - dist / 100));
                    gc.strokeLine(p1.x + p1.size / 2, p1.y + p1.size / 2, p2.x + p2.size / 2, p2.y + p2.size / 2);
                }
            }
        }
    }

    private double distance(Particle p1, Particle p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    private class Particle {
        double x, y;
        double dx, dy;
        double size;
        Color color;

        Particle(double w, double h) {
            reset(w, h);
        }

        void reset(double w, double h) {
            x = random.nextDouble() * w;
            y = random.nextDouble() * h;
            dx = (random.nextDouble() - 0.5) * 1.5;
            dy = (random.nextDouble() - 0.5) * 1.5;
            size = 2 + random.nextDouble() * 3;
            color = random.nextBoolean() ? Color.web(ACCENT_CYAN) : Color.web(ACCENT_PURPLE);
        }

        void update(double w, double h) {
            x += dx;
            y += dy;

            if (x < 0 || x > w || y < 0 || y > h) {
                // Bounce or wrap? Wrap is better for background
                if (x < 0)
                    x = w;
                if (x > w)
                    x = 0;
                if (y < 0)
                    y = h;
                if (y > h)
                    y = 0;
            }
        }
    }
}