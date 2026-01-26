//package client;
//
//import javafx.application.Application;
//import javafx.application.Platform;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.layout.*;
//import javafx.stage.Stage;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//public class GuiClient extends Application implements QuartoClient.GameListener {
//
//    private QuartoClient client;
//    private String username = "GuiPlayer";
//
//    // --- ЭЛЕМЕНТЫ ИНТЕРФЕЙСА ---
//    private Stage primaryStage;
//    private Label statusLabel;
//    private TextArea logArea; // Сюда будет падать Чат и RANK
//    private GridPane boardGrid;
//    private GridPane poolGrid;
//    private Button currentPieceDisplay; // Показывает фигуру, которую НАМ дали
//
//    // Массивы кнопок
//    private Button[] boardButtons = new Button[16];
//    private List<Button> poolButtons = new ArrayList<>();
//
//    // --- ЛОГИКА ИГРЫ ---
//    private boolean myTurn = false;
//    private int pieceToPlace = -1; // Фигура, которую мы держим в руке (нам дал соперник)
//    private int selectedLocation = -1; // Куда мы решили поставить фигуру (промежуточный шаг)
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage stage) {
//        this.primaryStage = stage;
//        this.client = new QuartoClient();
//
//        // 1. ВЕРХНЯЯ ПАНЕЛЬ (Подключение и Статус)
//        VBox topPanel = createTopPanel();
//
//        // 2. ЦЕНТРАЛЬНАЯ ЧАСТЬ (Доска и Пул фигур)
//        HBox gameArea = createGameArea();
//
//        // 3. НИЖНЯЯ ПАНЕЛЬ (Кнопки действий: Queue, Rank)
//        HBox bottomPanel = createBottomPanel();
//
//        // СБОРКА ОСНОВНОГО МАКЕТА
//        BorderPane root = new BorderPane();
//        root.setTop(topPanel);
//        root.setCenter(gameArea);
//        root.setBottom(bottomPanel);
//        root.setPadding(new Insets(10));
//        root.setStyle("-fx-background-color: #2b2b2b;"); // Темная тема
//
//        Scene scene = new Scene(root, 900, 600);
//        stage.setTitle("Quarto JavaFX Client");
//        stage.setScene(scene);
//        stage.show();
//    }
//
//    // --- СОЗДАНИЕ UI ---
//
//    private VBox createTopPanel() {
//        Label title = new Label("QUARTO ONLINE");
//        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
//
//        statusLabel = new Label("Disconnected");
//        statusLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 14px;");
//
//        // Поля ввода для логина/хоста
//        TextField hostField = new TextField("localhost");
//        TextField portField = new TextField("5432");
//        portField.setPrefWidth(60);
//        TextField userField = new TextField("Player" + (int)(Math.random()*100));
//
//        Button connectBtn = new Button("Connect");
//        connectBtn.setOnAction(e -> {
//            username = userField.getText();
//            connectToServer(hostField.getText(), Integer.parseInt(portField.getText()));
//        });
//
//        HBox connectionBox = new HBox(10, new Label("Host:") {{setTextFill(javafx.scene.paint.Color.WHITE);}}, hostField,
//                new Label("Port:") {{setTextFill(javafx.scene.paint.Color.WHITE);}}, portField,
//                new Label("User:") {{setTextFill(javafx.scene.paint.Color.WHITE);}}, userField,
//                connectBtn);
//        connectionBox.setAlignment(Pos.CENTER);
//        connectionBox.setPadding(new Insets(10));
//
//        VBox top = new VBox(10, title, statusLabel, connectionBox);
//        top.setAlignment(Pos.CENTER);
//        return top;
//    }
//
//    private HBox createGameArea() {
//        // --- ЛЕВАЯ ЧАСТЬ: ДОСКА ---
//        boardGrid = new GridPane();
//        boardGrid.setHgap(5);
//        boardGrid.setVgap(5);
//        boardGrid.setStyle("-fx-background-color: #333; -fx-padding: 10; -fx-background-radius: 5;");
//
//        for (int i = 0; i < 16; i++) {
//            Button btn = new Button();
//            btn.setPrefSize(70, 70);
//            btn.setStyle("-fx-font-size: 14px; -fx-base: #555;");
//            int idx = i;
//            btn.setOnAction(e -> handleBoardClick(idx));
//            boardButtons[i] = btn;
//            boardGrid.add(btn, i % 4, i / 4);
//        }
//
//        // --- ПРАВАЯ ЧАСТЬ: ИНФО И ФИГУРЫ ---
//        poolGrid = new GridPane();
//        poolGrid.setHgap(5);
//        poolGrid.setVgap(5);
//
//        for (int i = 0; i < 16; i++) {
//            Button btn = new Button(String.valueOf(i));
//            btn.setPrefSize(40, 40);
//            int pieceId = i;
//            btn.setOnAction(e -> handlePoolClick(pieceId));
//            poolButtons.add(btn);
//            poolGrid.add(btn, i % 4, i / 4);
//        }
//
//        currentPieceDisplay = new Button("?");
//        currentPieceDisplay.setPrefSize(80, 80);
//        currentPieceDisplay.setDisable(true);
//        currentPieceDisplay.setStyle("-fx-font-size: 24px; -fx-opacity: 1; -fx-border-color: #00e5ff; -fx-border-width: 2;");
//
//        logArea = new TextArea();
//        logArea.setEditable(false);
//        logArea.setPrefHeight(150);
//        logArea.setWrapText(true);
//        logArea.setStyle("-fx-control-inner-background: #222; -fx-text-fill: #00ff00;");
//
//        VBox rightSide = new VBox(15,
//                new Label("You must place:") {{setTextFill(javafx.scene.paint.Color.WHITE);}},
//                currentPieceDisplay,
//                new Label("Available Pieces (Pick for enemy):") {{setTextFill(javafx.scene.paint.Color.WHITE);}},
//                poolGrid,
//                new Label("Game Log / Chat / Rank:") {{setTextFill(javafx.scene.paint.Color.WHITE);}},
//                logArea
//        );
//        rightSide.setPadding(new Insets(0, 0, 0, 20));
//
//        HBox center = new HBox(boardGrid, rightSide);
//        center.setAlignment(Pos.CENTER);
//        return center;
//    }
//
//    private HBox createBottomPanel() {
//        Button btnQueue = new Button("Play (Queue)");
//        btnQueue.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
//        btnQueue.setOnAction(e -> {
//            client.queue();
//            log("System", "Joined queue...");
//        });
//
//        Button btnRank = new Button("Show Rankings");
//        btnRank.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
//        btnRank.setOnAction(e -> client.rankList()); // Вызываем метод клиента
//
//        Button btnList = new Button("List Players");
//        btnList.setOnAction(e -> client.listPlayers());
//
//        HBox bottom = new HBox(15, btnQueue, btnRank, btnList);
//        bottom.setAlignment(Pos.CENTER);
//        bottom.setPadding(new Insets(15));
//        return bottom;
//    }
//
//    // --- ЛОГИКА ВЗАИМОДЕЙСТВИЯ (CONTROLLER) ---
//
//    private void connectToServer(String host, int port) {
//        new Thread(() -> {
//            try {
//                client.connect(host, port, this);
//                client.login(username);
//            } catch (IOException e) {
//                Platform.runLater(() -> {
//                    statusLabel.setText("Connection Failed: " + e.getMessage());
//                    log("Error", e.getMessage());
//                });
//            }
//        }).start();
//    }
//
//    // 1. Клик по доске: Ставим фигуру, которую держим
//    private void handleBoardClick(int index) {
//        if (!myTurn) {
//            log("System", "Not your turn!");
//            return;
//        }
//        if (pieceToPlace == -1) {
//            log("System", "You don't have a piece to place yet."); // Редкий случай
//            return;
//        }
//        if (selectedLocation != -1) {
//            log("System", "You already placed the piece. Pick a piece for opponent!");
//            return;
//        }
//
//        // Визуально ставим фигуру (но пока не отправляем на сервер)
//        boardButtons[index].setText(String.valueOf(pieceToPlace));
//        boardButtons[index].setDisable(true);
//        selectedLocation = index; // Запоминаем, куда поставили
//
//        statusLabel.setText("STEP 2: Select a piece for your opponent from the list ->");
//        statusLabel.setStyle("-fx-text-fill: yellow; -fx-font-size: 16px;");
//    }
//
//    // 2. Клик по пулу фигур: Выбираем фигуру врагу и ОТПРАВЛЯЕМ ХОД
//    private void handlePoolClick(int pieceId) {
//        if (!myTurn) return;
//
//        // СЛУЧАЙ 1: Самое начало игры (я первый игрок).
//        // У меня нет фигуры в руке (pieceToPlace == -1), я должен только выбрать фигуру врагу.
//        if (pieceToPlace == -1) {
//            client.sendMove(-1, pieceId); // -1 значит "я ничего не поставил"
//            log("Me", "First move. Gave opponent piece " + pieceId);
//        }
//        // СЛУЧАЙ 2: Обычный ход.
//        // Я должен был сначала поставить фигуру на доску (selectedLocation != -1).
//        else {
//            if (selectedLocation == -1) {
//                log("System", "⚠️ Place your current piece (" + pieceToPlace + ") on the board first!");
//                return;
//            }
//            client.sendMove(selectedLocation, pieceId);
//            log("Me", "Placed " + pieceToPlace + " at " + selectedLocation + ", gave " + pieceId);
//        }
//
//        // Визуально отключаем кнопку выбранной фигуры в пуле
//        poolButtons.stream()
//                .filter(b -> b.getText().equals(String.valueOf(pieceId)))
//                .findFirst()
//                .ifPresent(b -> b.setDisable(true));
//
//        // Сбрасываем ход
//        myTurn = false;
//        pieceToPlace = -1;
//        selectedLocation = -1;
//        currentPieceDisplay.setText("");
//        statusLabel.setText("Opponent's turn...");
//        statusLabel.setStyle("-fx-text-fill: #00e5ff;");
//    }
//
//    private void log(String sender, String msg) {
//        Platform.runLater(() -> logArea.appendText("[" + sender + "] " + msg + "\n"));
//    }
//
//    // --- REALIZATION OF GAMELISTENER ---
//    // Все методы должны использовать Platform.runLater, так как вызываются из сети
//
//    @Override
//    public void onConnected() {
//        Platform.runLater(() -> {
//            statusLabel.setText("Connected as " + username);
//            statusLabel.setStyle("-fx-text-fill: #00ff00;");
//            log("System", "Connected successfully.");
//        });
//    }
//
//    @Override
//    public void onNewGame(String p1, String p2) {
//        Platform.runLater(() -> {
//            log("System", "NEW GAME: " + p1 + " vs " + p2);
//            statusLabel.setText("Game Started!");
//
//            // Сброс UI
//            for (Button b : boardButtons) {
//                b.setText("");
//                b.setDisable(false);
//            }
//            for (Button b : poolButtons) {
//                b.setDisable(false);
//            }
//
//            selectedLocation = -1;
//
//            if (p1.equals(username)) {
//                myTurn = true;
//                pieceToPlace = -1; // Первый игрок не ставит фигуру, а выбирает
//                // Для первого хода особый хак: мы сразу переходим к шагу 2
//                // Но нам нужно заблокировать доску, чтобы он не тыкал
//                statusLabel.setText("YOUR TURN: Pick a piece for opponent!");
//                // Мы "как бы" уже поставили фигуру в никуда (-1)
//                selectedLocation = -1;
//                // Но логика handlePoolClick требует selectedLocation != -1 для обычного хода
//                // Поэтому для первого хода нужна оговорка.
//                // В методе handlePoolClick мы проверяем selectedLocation.
//                // При первом ходе sendMove(-1, piece)
//                // Давай поправим handlePoolClick для этого случая ниже.
//            } else {
//                myTurn = false;
//                statusLabel.setText("Opponent starts...");
//            }
//        });
//    }
//
//    // Исправление handlePoolClick для самого первого хода
//    // (Я переписываю его логику здесь, чтобы ты заменил метод выше, если нужно,
//    // но лучше просто учтем это: если pieceToPlace == -1 и myTurn == true, значит это первый ход)
//    /*
//     * ВАЖНО: Вставь это ВМЕСТО того метода handlePoolClick, что выше,
//     * чтобы правильно обработать первый ход игры
//     */
//    /*
//    private void handlePoolClick(int pieceId) {
//        if (!myTurn) return;
//
//        // Спец. случай: Первый ход игры (мы еще ничего не ставим)
//        if (pieceToPlace == -1) {
//             client.sendMove(-1, pieceId);
//             log("Me", "Start game. Gave " + pieceId);
//        } else {
//             // Обычный ход
//             if (selectedLocation == -1) {
//                 log("System", "Place piece on board first!");
//                 return;
//             }
//             client.sendMove(selectedLocation, pieceId);
//             log("Me", "Placed " + pieceToPlace + ", gave " + pieceId);
//        }
//
//        // Блокируем кнопку
//        poolButtons.stream().filter(b -> b.getText().equals(String.valueOf(pieceId)))
//                   .findFirst().ifPresent(b -> b.setDisable(true));
//
//        myTurn = false;
//        pieceToPlace = -1;
//        selectedLocation = -1;
//        currentPieceDisplay.setText("");
//        statusLabel.setText("Opponent's turn...");
//    }
//    */
//    // ТАК КАК Я НЕ МОГУ РЕДАКТИРОВАТЬ КОД ВНУТРИ БЛОКА ВЫШЕ, ВОТ ПОЛНАЯ ВЕРСИЯ handlePoolClick:
//    /* ВСТАВЬ ЭТО В КОД ВМЕСТО СТАРОГО handlePoolClick */
//
//    /* * P.S. Я перепишу его ниже в отдельном блоке, чтобы ты скопировал.
//     * Но пока продолжим GameListener
//     */
//
//    @Override
//    public void onOpponentMove(int location, int piece) {
//        Platform.runLater(() -> {
//            // 1. Отображаем ход противника
//            if (location != -1) {
//                // Он поставил фигуру pieceToPlace (которую мы ему дали в прошлый раз)
//                // Но мы могли забыть, какая она была.
//                // Проще: сервер присылает location и nextPiece.
//                // А какую фигуру он поставил?
//                // Протокол: MOVE~location~nextPiece.
//                // Сервер не говорит, ЧТО он поставил. Но мы знаем, что было у него в руке.
//                // В GUI это сложно отследить без локального стейта игры.
//                // ХАК: Просто ставим "X" или обновляем доску полностью, если бы сервер слал состояние.
//                // НО: у нас нет инфы о поставленной фигуре в аргументах onOpponentMove.
//                // Придется верить, что мы помним `currentPieceDisplay`.
//
//                // В GuiClient мы хранили pieceToPlace (наше).
//                // У врага было то, что мы ему дали.
//                // Давай просто пометим клетку как занятую пока что.
//                boardButtons[location].setText("X"); // Или ID, если бы мы хранили историю
//                boardButtons[location].setDisable(true);
//            }
//
//            // 2. Теперь наша очередь ставить `piece`
//            pieceToPlace = piece;
//            myTurn = true;
//            selectedLocation = -1;
//
//            currentPieceDisplay.setText(String.valueOf(piece));
//
//            // Удаляем эту фигуру из пула доступных (ее больше нельзя выбрать для врага)
//            poolButtons.stream().filter(b -> b.getText().equals(String.valueOf(piece)))
//                    .findFirst().ifPresent(b -> b.setDisable(true));
//
//            statusLabel.setText("YOUR TURN: Place " + piece + " on board!");
//            statusLabel.setStyle("-fx-text-fill: #00ff00;");
//        });
//    }
//
//    @Override
//    public void onGameOver(String result, String winner) {
//        Platform.runLater(() -> {
//            statusLabel.setText("GAME OVER: " + result);
//            if (winner != null && !winner.isEmpty()) {
//                log("System", "Winner is " + winner);
//            }
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("Game Over");
//            alert.setHeaderText("Result: " + result);
//            alert.setContentText("Winner: " + winner);
//            alert.show();
//        });
//    }
//
//    @Override
//    public void onError(String msg) {
//        Platform.runLater(() -> {
//            log("Error", msg);
//            statusLabel.setText("Error: " + msg);
//        });
//    }
//
//    @Override
//    public void onChat(String sender, String text) {
//        // Твой QuartoClient вызывает это для RANK, CHAT и SYSTEM сообщений
//        log(sender, text);
//    }
//}