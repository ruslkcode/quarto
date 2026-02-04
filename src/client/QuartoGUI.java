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

import java.util.ArrayList;
import java.util.List;

public class QuartoGUI extends Application {

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

    // --- СОСТОЯНИЕ ИГРЫ ---
    private boolean isMyTurn = false;
    private boolean waitingForServerEcho = false;

    private int pieceToPlace = -1;       // Фигура, которую нам ДАЛИ (нужно поставить)
    private int lastPlacedPiece = -1;    // Вспомогательная для синхронизации
    private int pieceGivenToOpponent = -1; // Фигура, которую МЫ дали оппоненту

    private Integer[] localBoard = new Integer[16];
    private Button[] boardButtons = new Button[16];

    // --- СТИЛИ (Neon Cyberpunk) ---
    private static final String BG_COLOR = "#1e1e1e";
    private static final String PANEL_COLOR = "#2d2d2d";
    private static final String ACCENT_COLOR = "#00bcd4";
    private static final String WIN_COLOR = "#00ff00";
    private static final String LOSE_COLOR = "#ff0000";

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
        connectBtn.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 20;");
        connectBtn.setPrefSize(220, 45);
        connectBtn.setOnAction(e -> handleConnect());

        content.getChildren().addAll(title, nameField, ipField, portField, connectBtn);
        primaryStage.setScene(new Scene(content, 400, 550));
    }

    private void handleConnect() {
        this.myUsername = nameField.getText().trim();
        new Thread(() -> {
            try {
                client.connect(ipField.getText(), Integer.parseInt(portField.getText()), createGameListener());
                client.send("HELLO~GuiClient");
                client.login(myUsername);
                Platform.runLater(this::showQueueScreen);
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void showQueueScreen() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");
        Button joinBtn = new Button("FIND MATCH");
        joinBtn.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-font-weight: bold; -fx-background-radius: 20;");
        joinBtn.setPrefSize(200, 50);
        joinBtn.setOnAction(e -> client.queue());
        root.getChildren().addAll(new Label("LOBBY"), joinBtn);
        primaryStage.setScene(new Scene(root, 400, 400));
    }

    private void showGameScreen() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: " + BG_COLOR + ";");

        statusLabel = new Label("Game Started");
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        HBox topBar = new HBox(statusLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(15));
        topBar.setStyle("-fx-background-color: " + PANEL_COLOR + ";");
        layout.setTop(topBar);

        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(10); boardGrid.setVgap(10);
        for (int i = 0; i < 16; i++) {
            Button btn = new Button();
            btn.setPrefSize(85, 85);
            btn.setStyle("-fx-background-color: #333; -fx-background-radius: 10; -fx-border-color: #444;");
            int finalI = i;
            btn.setOnAction(e -> handleBoardClick(finalI));
            boardButtons[i] = btn;
            boardGrid.add(btn, i % 4, i / 4);
        }
        layout.setCenter(boardGrid);

        VBox rightBar = new VBox(20);
        rightBar.setPadding(new Insets(20));
        rightBar.setStyle("-fx-background-color: " + PANEL_COLOR + ";");
        currentPieceBox = new VBox(10);
        currentPieceBox.setAlignment(Pos.CENTER);
        availablePiecesPane = new FlowPane();
        availablePiecesPane.setHgap(8); availablePiecesPane.setVgap(8);
        availablePiecesPane.setPrefWrapLength(250);

        rightBar.getChildren().addAll(new Label("PLACE THIS:"), currentPieceBox, new Separator(), new Label("AVAILABLE:"), availablePiecesPane);
        layout.setRight(rightBar);

        rootPane = new StackPane(layout);
        primaryStage.setScene(new Scene(rootPane, 1100, 750));
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
                        statusLabel.setText("Your Turn: Give a piece!");
                        refreshAvailablePieces();
                        disableBoard();
                    } else {
                        isMyTurn = false;
                        statusLabel.setText("Waiting for opponent...");
                        availablePiecesPane.setDisable(true);
                    }
                });
            }

            @Override
            public void onOpponentMove(int location, int nextPieceId) {
                Platform.runLater(() -> {
                    boolean wasMyMove = waitingForServerEcho;
                    waitingForServerEcho = false;

                    // Обновляем доску
                    if (location >= 0) {
                        int placedId = wasMyMove ? lastPlacedPiece : pieceGivenToOpponent;
                        localBoard[location] = placedId;
                        boardButtons[location].setGraphic(createPieceShape(placedId));
                        boardButtons[location].setDisable(true);
                        boardButtons[location].setStyle("-fx-background-color: #222; -fx-opacity: 1;");
                    }

                    if (wasMyMove) {
                        isMyTurn = false;
                        statusLabel.setText("Waiting for opponent...");
                        disableBoard();
                        availablePiecesPane.setDisable(true);
                    } else {
                        isMyTurn = true;
                        pieceToPlace = nextPieceId;
                        statusLabel.setText("Your Turn: Place the piece!");
                        currentPieceBox.getChildren().setAll(createPieceShape(pieceToPlace));
                        enableEmptyBoard();
                        availablePiecesPane.setDisable(true);
                    }
                    refreshAvailablePieces();
                });
            }

            @Override
            public void onGameOver(String res, String win) {
                Platform.runLater(() -> {
                    boolean iWon = win.trim().equalsIgnoreCase(myUsername.trim());
                    showEndOverlay(iWon ? "VICTORY" : "DEFEAT", "Winner: " + win, iWon ? WIN_COLOR : LOSE_COLOR);
                });
            }

            @Override public void onConnected() {}
            @Override public void onError(String msg) { Platform.runLater(() -> statusLabel.setText("Error: " + msg)); }
            @Override public void onChat(String s, String t) {}
        };
    }

    private void handleBoardClick(int index) {
        if (!isMyTurn || pieceToPlace == -1) return;

        lastPlacedPiece = pieceToPlace;
        localBoard[index] = lastPlacedPiece;
        boardButtons[index].setGraphic(createPieceShape(lastPlacedPiece));

        // АВТО-ПРОВЕРКА ПОБЕДЫ
        if (checkLocalWin()) {
            waitingForServerEcho = true;
            client.sendMove(index, 16); // Отправляем 16 серверу для победы
            statusLabel.setText("Winning...");
        } else {
            pieceToPlace = -1;
            currentPieceBox.getChildren().clear();
            statusLabel.setText("Now pick a piece for opponent!");
            disableBoard();
            availablePiecesPane.setDisable(false);
        }
    }

    private void handlePieceSelect(int pieceId) {
        if (!isMyTurn) return;
        boolean isStart = true;
        for(Integer p : localBoard) if(p != null) isStart = false;

        waitingForServerEcho = true;
        client.sendMove(isStart ? -1 : findLastLocation(), pieceId);
        pieceGivenToOpponent = pieceId;
    }

    private int findLastLocation() {
        for(int i=0; i<16; i++) if(localBoard[i] != null && boardButtons[i].isDisabled() && localBoard[i] == lastPlacedPiece) return i;
        return -1;
    }

    private boolean checkLocalWin() {
        int[][] lines = {{0,1,2,3},{4,5,6,7},{8,9,10,11},{12,13,14,15},{0,4,8,12},{1,5,9,13},{2,6,10,14},{3,7,11,15},{0,5,10,15},{3,6,9,12}};
        for (int[] l : lines) {
            if (localBoard[l[0]]==null || localBoard[l[1]]==null || localBoard[l[2]]==null || localBoard[l[3]]==null) continue;
            int p1=localBoard[l[0]], p2=localBoard[l[1]], p3=localBoard[l[2]], p4=localBoard[l[3]];
            if (((p1&p2&p3&p4) != 0) || (((~p1)&(~p2)&(~p3)&(~p4)&0xf) != 0)) return true;
        }
        return false;
    }

    private void refreshAvailablePieces() {
        availablePiecesPane.getChildren().clear();
        for (int i = 0; i < 16; i++) {
            boolean used = false;
            for(Integer p : localBoard) if(p != null && p == i) used = true;
            if (used || i == pieceToPlace) continue;
            final int pId = i;
            Button b = new Button();
            b.setGraphic(createPieceShape(i));
            b.setStyle("-fx-background-color: transparent; -fx-border-color: #444;");
            b.setOnAction(e -> handlePieceSelect(pId));
            availablePiecesPane.getChildren().add(b);
        }
    }

    private void resetGameState() {
        waitingForServerEcho = false;
        pieceToPlace = -1; lastPlacedPiece = -1; pieceGivenToOpponent = -1;
        localBoard = new Integer[16];
    }

    private void disableBoard() { for (Button b : boardButtons) if (b != null) b.setDisable(true); }
    private void enableEmptyBoard() { for (int i = 0; i < 16; i++) if (localBoard[i] == null) boardButtons[i].setDisable(false); }

    private StackPane createPieceShape(int id) {
        boolean hollow=(id&8)!=0, square=(id&4)!=0, tall=(id&2)!=0, dark=(id&1)!=0;
        Shape s = square ? new Rectangle(tall?60:35, tall?60:35) : new Circle(tall?30:18);
        Color c = dark ? Color.web("#9c27b0") : Color.web("#ff9800");
        s.setFill(hollow ? Color.TRANSPARENT : c);
        s.setStroke(c); s.setStrokeWidth(3);
        s.setEffect(new DropShadow(15, c));
        return new StackPane(s);
    }

    private void showEndOverlay(String title, String sub, String color) {
        VBox overlay = new VBox(20); overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
        Label l1 = new Label(title); l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 60)); l1.setTextFill(Color.web(color));
        Label l2 = new Label(sub); l2.setTextFill(Color.WHITE);
        Button btn = new Button("PLAY AGAIN"); btn.setOnAction(e -> { client.queue(); showQueueScreen(); });
        overlay.getChildren().addAll(l1, l2, btn);
        rootPane.getChildren().add(overlay);
    }

    private TextField createStyledTextField(String p) {
        TextField t = new TextField(); t.setPromptText(p);
        t.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 10;");
        return t;
    }
}