package gameLogic;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the game logic of a Quarto game.
 * This class maintains the game state, including the board,
 * the current player, the current piece to be placed,
 * and the available pieces.
 */
public class Game {

    private Board board;
    private int currentPlayer;
    private int currentPieceID; // id of the piece that must be placed
    private Map<Integer, Piece> allPieces;
    private Map<Integer, Piece> availablePieces;

    /*@
      private invariant board != null;
      private invariant currentPlayer == 1 || currentPlayer == 2;
      private invariant allPieces != null;
      private invariant availablePieces != null;
    @*/

    /**
     * Constructs a new Quarto game.
     * Initializes an empty board, sets the starting player,
     * and creates all available pieces.
     *
     * @param currentPlayer the player who starts the game (1 or 2)
     */
    /*@
      requires currentPlayer == 1 || currentPlayer == 2;
      ensures getCurrentPlayer() == currentPlayer;
    @*/
    public Game(int currentPlayer) {
        board = new Board();
        this.currentPlayer = currentPlayer;
        currentPieceID = -1;
        allPieces = new HashMap<>();
        availablePieces = new HashMap<>();
        initPieces();
    }

    /**
     * Initializes all 16 unique pieces used in the game
     * and stores them as available pieces.
     */
    /*@
      ensures allPieces.size() == 16;
      ensures availablePieces.size() == 16;
    @*/
    private void initPieces() {
        int id = 0;
        for (Size size : Size.values()) {
            for (Shape shape : Shape.values()) {
                for (Colour colour : Colour.values()) {
                    for (Fill fill : Fill.values()) {
                        Piece piece = new Piece(size, shape, colour, fill);
                        allPieces.put(id, piece);
                        availablePieces.put(id, piece);
                        id++;
                    }
                }
            }
        }
    }

    /**
     * Returns the player whose turn it currently is.
     *
     * @return the current player (1 or 2)
     */
    /*@
      ensures \result == 1 || \result == 2;
    @*/
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Returns the winner of the game.
     *
     * @return the id of the winning player, or 0 if there is no winner
     */
    /*@
      ensures \result == 0 || \result == 1 || \result == 2;
    @*/
    public int getWinner() {
        return board.hasWinner() ? currentPlayer : 0;
    }

    /**
     * Checks whether the given move is valid according to the game rules.
     *
     * @param m the move to be checked
     * @return true if the move is valid, false otherwise
     */
    /*@
      requires m != null;
    @*/
    public boolean isValidMove(Move m) {

        // first move: only choosing a piece
        if (m.isFirstMove()) {
            return currentPieceID == -1
                    && availablePieces.containsKey(m.getNextPiece());
        }

        // regular move
        if (currentPieceID == -1) return false;
        if (!board.isField(m.getLocation())) return false;
        if (!board.isEmptyField(m.getLocation())) return false;
        if (!availablePieces.containsKey(m.getNextPiece())) return false;

        return true;
    }

    /**
     * Executes the given move.
     * Updates the board, removes used pieces, and switches the player turn.
     *
     * @param m the move to execute
     */
    /*@
      requires m != null;
      requires isValidMove(m);
      ensures getCurrentPlayer() != \old(getCurrentPlayer());
    @*/
    public void doMove(Move m) {

        // first move: only select a piece
        if (m.isFirstMove()) {
            currentPieceID = m.getNextPiece();
            availablePieces.remove(currentPieceID);
            switchPlayer();
            return;
        }

        // regular move
        board.setField(m.getLocation(), allPieces.get(currentPieceID));
        availablePieces.remove(currentPieceID);

        currentPieceID = m.getNextPiece();
        availablePieces.remove(currentPieceID);

        switchPlayer();
    }

    /**
     * Checks whether the game is over.
     * The game is over if there is a winner or the board is full.
     *
     * @return true if the game is over, false otherwise
     */
    /*@
      ensures \result == (getWinner() != 0 || board.isFull());
    @*/
    public boolean isGameOver() {
        return getWinner() != 0 || board.isFull();
    }

    /**
     * Switches the turn to the other player.
     */
    private void switchPlayer() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }
}
