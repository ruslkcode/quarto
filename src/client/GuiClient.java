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

import java.util.ArrayList;
import java.util.List;

public class GuiClient extends Application {

    private Stage primaryStage;
    private GridPane boardGrid;
    private FlowPane availablePiecesPane;
    private Label statusLabel;
    private VBox currentPieceBox;
    private StackPane rootPane;

    private TextField ipField;
    private TextField portField;
    private TextField nameField;

    private QuartoClient client;
    private String myUsername;

    // --- STATE ---
    private boolean isMyTurn = false;
    private boolean amIPlayer1 = false;
    private boolean waitingForServerEcho = false;

    private int pieceToPlace = -1;
    private int placedLocation = -1;
    private int pieceGivenToOpponent = -1;

    // Локальное состояние доски
    private Integer[] localBoard = new Integer[16];
    private List<Integer> usedPieces = new ArrayList<>();
    private Button[] boardButtons = new Button[16];

    // --- COLORS ---
    private static final String BG_COLOR = "#1e1e1e";
    private static final String PANEL_COLOR = "#2d2d2d";
    private static final String ACCENT_COLOR = "#00bcd4";
    private static final String WIN_COLOR = "#00ff00";
    private static final String LOSE_COLOR = "#ff0000";
    private static final String GOLD_COLOR = "#FFD700";

    private static final String BTN_CYAN = "-fx-background-color: " + ACCENT_COLOR + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";
    private static final String BTN_CYAN_HOVER = "-fx-background-color: #00e5ff; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.client = new QuartoClient();
        primaryStage.setTitle("Quarto | Portfolio Project");
        showLoginScreen();
        primaryStage.show();
    }

    // ==========================================
    //              SCREENS
    // ==========================================
    private void showLoginScreen() {
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: " + BG_COLOR + ";");

        Label title = new Label("QUARTO");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        title.setTextFill(Color.web(ACCENT_COLOR));
        title.setEffect(new DropShadow(20, Color.web(ACCENT_COLOR)));

        nameField = createStyledTextField("Player1");
        ipField = createStyledTextField("localhost");
        portField = createStyledTextField("5432");

        Button connectBtn = new Button("CONNECT TO SERVER");
        connectBtn.setPrefSize(220, 45);
        styleButton(connectBtn, BTN_CYAN, BTN_CYAN_HOVER);

        connectBtn.setOnAction(e -> handleConnect());

        content.getChildren().addAll(title, nameField, ipField, portField, connectBtn);
        primaryStage.setScene(new Scene(content, 400, 550));
    }

    private void handleConnect() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();
        int port;
        try { port = Integer.parseInt(portField.getText().trim()); } catch(Exception e) { return; }
        if (name.isEmpty()) return;
        this.myUsername = name;

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");
        ProgressIndicator spinner = new ProgressIndicator();
        root.getChildren().addAll(spinner, new Label("Connecting..."));
        primaryStage.setScene(new Scene(root, 400, 300));

        new Thread(() -> {
            try {
                client.connect(ip, port, createGameListener());
                client.login(name);
                Platform.runLater(this::showQueueScreen);
            } catch (Exception ex) {
                Platform.runLater(this::showLoginScreen);
            }
        }).start();
    }

    private void showQueueScreen() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        Label title = new Label("LOBBY");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        title.setTextFill(Color.WHITE);
        Label sub = new Label("Logged in as: " + myUsername);
        sub.setTextFill(Color.GRAY);

        Button joinBtn = new Button("FIND MATCH");
        joinBtn.setPrefSize(200, 50);
        styleButton(joinBtn, BTN_CYAN, BTN_CYAN_HOVER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setVisible(false);
        Label statusLbl = new Label("");
        statusLbl.setTextFill(Color.GRAY);

        joinBtn.setOnAction(e -> {
            client.queue();
            joinBtn.setDisable(true);
            spinner.setVisible(true);
            statusLbl.setText("Searching for opponent...");
        });

        root.getChildren().addAll(title, sub, joinBtn, spinner, statusLbl);
        primaryStage.setScene(new Scene(root, 500, 400));
    }

    private void showGameScreen() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: " + BG_COLOR + ";");

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(15));
        topBar.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");
        statusLabel = new Label("Game Started");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.WHITE);
        topBar.getChildren().add(statusLabel);
        layout.setTop(topBar);

        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(10);
        boardGrid.setVgap(10);
        boardGrid.setPadding(new Insets(20));

        for (int i = 0; i < 16; i++) {
            Button btn = new Button();
            btn.setPrefSize(80, 80);
            btn.setStyle("-fx-background-color: #333; -fx-background-radius: 10; -fx-border-color: #444; -fx-border-radius: 10;");

            int finalI = i;
            btn.setOnMouseEntered(e -> {
                if (!btn.isDisabled()) btn.setStyle("-fx-background-color: #444; -fx-background-radius: 10; -fx-border-color: " + ACCENT_COLOR + "; -fx-border-radius: 10;");
            });
            btn.setOnMouseExited(e -> {
                if (!btn.isDisabled()) btn.setStyle("-fx-background-color: #333; -fx-background-radius: 10; -fx-border-color: #444; -fx-border-radius: 10;");
            });

            btn.setOnAction(e -> handleBoardClick(finalI));
            boardButtons[i] = btn;
            boardGrid.add(btn, i % 4, i / 4);
        }
        layout.setCenter(boardGrid);

        VBox rightBar = new VBox(20);
        rightBar.setPadding(new Insets(20));
        rightBar.setMinWidth(320);
        rightBar.setStyle("-fx-background-color: " + PANEL_COLOR + ";");
        rightBar.setAlignment(Pos.TOP_CENTER);

        currentPieceBox = new VBox(10);
        currentPieceBox.setAlignment(Pos.CENTER);
        currentPieceBox.setPadding(new Insets(15));
        currentPieceBox.setStyle("-fx-background-color: #222; -fx-background-radius: 10; -fx-border-color: #444; -fx-border-radius: 10;");
        Label lblCurrent = new Label("CURRENT PIECE");
        lblCurrent.setTextFill(Color.GRAY);
        lblCurrent.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        currentPieceBox.getChildren().add(lblCurrent);

        Label lblAvail = new Label("AVAILABLE PIECES");
        lblAvail.setTextFill(Color.GRAY);
        lblAvail.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        availablePiecesPane = new FlowPane();
        availablePiecesPane.setAlignment(Pos.CENTER);
        availablePiecesPane.setHgap(8);
        availablePiecesPane.setVgap(8);

        rightBar.getChildren().addAll(currentPieceBox, new Separator(), lblAvail, availablePiecesPane);
        layout.setRight(rightBar);

        rootPane = new StackPane(layout);
        primaryStage.setScene(new Scene(rootPane, 1100, 750));
    }

    private void showGameOverScreen(String result, String winner) {
        VBox overlay = new VBox(20);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);");

        Label resLbl = new Label();
        resLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 50));

        boolean iAmWinner = winner.trim().equalsIgnoreCase(myUsername.trim());

        if (iAmWinner) {
            resLbl.setText("VICTORY");
            resLbl.setTextFill(Color.web(WIN_COLOR));
            resLbl.setEffect(new DropShadow(30, Color.web(WIN_COLOR)));
        } else if (result.contains("DRAW")) {
            resLbl.setText("DRAW");
            resLbl.setTextFill(Color.WHITE);
        } else {
            resLbl.setText("DEFEAT");
            resLbl.setTextFill(Color.web(LOSE_COLOR));
            resLbl.setEffect(new DropShadow(30, Color.web(LOSE_COLOR)));
        }

        Label winLbl = new Label("Winner: " + winner);
        winLbl.setFont(Font.font("Segoe UI", 20));
        winLbl.setTextFill(Color.LIGHTGRAY);

        Button playAgainBtn = new Button("PLAY AGAIN");
        playAgainBtn.setPrefSize(200, 50);
        styleButton(playAgainBtn, BTN_CYAN, BTN_CYAN_HOVER);

        playAgainBtn.setOnAction(e -> {
            client.queue();
            showQueueScreen();
        });

        overlay.getChildren().addAll(resLbl, winLbl, playAgainBtn);
        rootPane.getChildren().add(overlay);
    }

    // ==========================================
    //              LOGIC
    // ==========================================
    private QuartoClient.GameListener createGameListener() {
        return new QuartoClient.GameListener() {
            @Override
            public void onConnected() {}

            @Override
            public void onNewGame(String p1, String p2) {
                Platform.runLater(() -> {
                    resetGameState();
                    showGameScreen();

                    if (myUsername.equals(p1)) {
                        amIPlayer1 = true;
                        isMyTurn = true;
                        pieceToPlace = -1;
                        updateStatus("YOUR TURN! Select piece for opponent.", Color.web(ACCENT_COLOR));
                        refreshAvailablePieces();
                        disableBoard();
                    } else {
                        amIPlayer1 = false;
                        isMyTurn = false;
                        updateStatus("Waiting for opponent...", Color.GRAY);
                        refreshAvailablePieces();
                        availablePiecesPane.setDisable(true);
                        disableBoard();
                    }
                });
            }

            @Override
            public void onOpponentMove(int location, int nextPieceId) {
                Platform.runLater(() -> {
                    if (waitingForServerEcho) { waitingForServerEcho = false; return; }

                    if (location >= 0 && location < 16) {
                        localBoard[location] = pieceGivenToOpponent;
                        Button btn = boardButtons[location];
                        if (pieceGivenToOpponent != -1) btn.setGraphic(createPieceShape(pieceGivenToOpponent));
                        else btn.setText("X");
                        btn.setDisable(true);
                        btn.setStyle("-fx-background-color: #222; -fx-opacity: 1;");
                    }

                    isMyTurn = true;
                    pieceToPlace = nextPieceId;
                    placedLocation = -1;
                    usedPieces.add(pieceToPlace);
                    refreshAvailablePieces();

                    updateStatus("YOUR TURN! Place the piece on the board.", Color.web(ACCENT_COLOR));

                    currentPieceBox.getChildren().clear();
                    Label lbl = new Label("PLACE THIS ON BOARD:");
                    lbl.setTextFill(Color.WHITE);
                    currentPieceBox.getChildren().addAll(lbl, createPieceShape(pieceToPlace));

                    enableEmptyBoard();
                    availablePiecesPane.setDisable(true);
                });
            }

            @Override
            public void onGameOver(String result, String winner) {
                Platform.runLater(() -> showGameOverScreen(result, winner));
            }
            @Override
            public void onError(String msg) {
                Platform.runLater(() -> statusLabel.setText("Error: " + msg));
            }
            @Override
            public void onChat(String s, String t) {}
        };
    }

    private void handleBoardClick(int index) {
        if (!isMyTurn || pieceToPlace == -1) return;

        localBoard[index] = pieceToPlace;
        Button btn = boardButtons[index];
        btn.setGraphic(createPieceShape(pieceToPlace));
        btn.setDisable(true);
        btn.setStyle("-fx-background-color: #222; -fx-opacity: 1; -fx-border-color: " + ACCENT_COLOR + ";");

        placedLocation = index;
        pieceToPlace = -1;

        currentPieceBox.getChildren().clear();
        Label lbl = new Label("Piece Placed.");
        lbl.setTextFill(Color.GRAY);
        currentPieceBox.getChildren().add(lbl);
        disableBoard();

        // Проверяем победу для красоты интерфейса
        boolean isWin = checkLocalWin();
        if (isWin) {
            updateStatus("VICTORY! Select ANY piece to finish.", Color.web(GOLD_COLOR));
        } else {
            updateStatus("Select piece for opponent ->", Color.WHITE);
        }

        // ВСЕГДА разрешаем выбрать фигуру. Это требование сервера.
        availablePiecesPane.setDisable(false);
    }

    private void handlePieceSelect(int pieceId) {
        if (!isMyTurn) return;
        boolean isFirstMove = (amIPlayer1 && placedLocation == -1 && pieceGivenToOpponent == -1);
        boolean hasPlaced = (placedLocation != -1);

        if (isFirstMove || hasPlaced) {
            waitingForServerEcho = true;
            int locToSend = isFirstMove ? -1 : placedLocation;

            // Отправляем ход. Если это был выигрышный ход, сервер сам заметит линию и пришлет GAMEOVER.
            client.sendMove(locToSend, pieceId);

            pieceGivenToOpponent = pieceId;
            usedPieces.add(pieceId);
            refreshAvailablePieces();

            isMyTurn = false;
            amIPlayer1 = false;
            placedLocation = -1;

            updateStatus("Waiting for opponent...", Color.GRAY);
            disableBoard();
            availablePiecesPane.setDisable(true);
        }
    }

    private boolean checkLocalWin() {
        int[][] lines = {
                {0,1,2,3}, {4,5,6,7}, {8,9,10,11}, {12,13,14,15},
                {0,4,8,12}, {1,5,9,13}, {2,6,10,14}, {3,7,11,15},
                {0,5,10,15}, {3,6,9,12}
        };

        for (int[] line : lines) {
            if (localBoard[line[0]] == null || localBoard[line[1]] == null ||
                    localBoard[line[2]] == null || localBoard[line[3]] == null) continue;

            int p1 = localBoard[line[0]];
            int p2 = localBoard[line[1]];
            int p3 = localBoard[line[2]];
            int p4 = localBoard[line[3]];

            int commonAnd = (p1 & p2 & p3 & p4);
            int commonNor = ((~p1) & (~p2) & (~p3) & (~p4)) & 0x0F;

            if (commonAnd != 0 || commonNor != 0) return true;
        }
        return false;
    }

    private void refreshAvailablePieces() {
        availablePiecesPane.getChildren().clear();
        for (int i = 0; i < 16; i++) {
            if (usedPieces.contains(i)) continue;
            final int pId = i;
            Button btn = new Button();
            btn.setGraphic(createPieceShape(i));
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #444; -fx-border-radius: 5;");

            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #333; -fx-border-color: " + ACCENT_COLOR + "; -fx-border-radius: 5;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: #444; -fx-border-radius: 5;"));

            btn.setOnAction(e -> handlePieceSelect(pId));
            availablePiecesPane.getChildren().add(btn);
        }
    }

    private void styleButton(Button btn, String styleNormal, String styleHover) {
        btn.setStyle(styleNormal);
        btn.setOnMouseEntered(e -> { if(!btn.isDisabled()) btn.setStyle(styleHover); });
        btn.setOnMouseExited(e -> { if(!btn.isDisabled()) btn.setStyle(styleNormal); });
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-prompt-text-fill: gray; -fx-background-radius: 5;");
        tf.setPrefHeight(35);
        tf.setMaxWidth(300);
        return tf;
    }

    private void resetGameState() {
        waitingForServerEcho = false;
        pieceGivenToOpponent = -1;
        placedLocation = -1;
        usedPieces.clear();
        localBoard = new Integer[16];
    }

    private void updateStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(color);
    }

    private void disableBoard() {
        for (Button b : boardButtons) if (b != null) b.setDisable(true);
    }

    private void enableEmptyBoard() {
        for (Button b : boardButtons) {
            if (b != null && b.getText().isEmpty() && b.getGraphic() == null) {
                b.setDisable(false);
                b.setStyle("-fx-background-color: #333333; -fx-background-radius: 10; -fx-border-color: #444; -fx-opacity: 1;");
            }
        }
    }

    // Совпадение логики битов с сервером
    // Bit 0 = Fill, Bit 1 = Color, Bit 2 = Shape, Bit 3 = Size
    private StackPane createPieceShape(int id) {
        boolean isHollow = (id & 1) != 0;
        boolean isDark   = (id & 2) == 0;
        boolean isRound  = (id & 4) == 0;
        boolean isBig    = (id & 8) == 0;

        Shape shape;
        double size = isBig ? 35 : 20;

        if (isRound) shape = new Circle(size);
        else shape = new Rectangle(size * 2, size * 2);

        Color strokeColor = isDark ? Color.web("#9c27b0") : Color.web("#ff9800");
        Color fillColor = isHollow ? Color.TRANSPARENT : strokeColor;

        shape.setFill(fillColor);
        shape.setStroke(strokeColor);
        shape.setStrokeWidth(3);

        DropShadow glow = new DropShadow();
        glow.setColor(strokeColor);
        glow.setRadius(15);
        glow.setSpread(0.2);
        shape.setEffect(glow);

        StackPane container = new StackPane(shape);
        String binaryString = String.format("%4s", Integer.toBinaryString(id)).replace(' ', '0');
        Label debugLbl = new Label(binaryString);
        debugLbl.setTextFill(isHollow ? strokeColor : Color.WHITE);
        debugLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-effect: dropshadow(one-pass-box, black, 3, 1.0, 0, 0);");
        container.getChildren().add(debugLbl);

        return container;
    }
}