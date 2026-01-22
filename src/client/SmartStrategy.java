package client;

import gameLogic.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SmartStrategy implements BotStrategy{
    @Override
    public String getName() {
        return "Smart";
    }

    /**
     * Determines the next move for the AI using a layered decision strategy.
     * @param game the current game state
     * @return the selected move according to the strategy
     */

    /*@
       requires game != null;
       requires !game.isGameOver();
       ensures \result != null;
       ensures game.isValidMove(\result);
     @*/

    @Override
    public Move determineMove(Game game) {

        // Immediate win
        Move win = findImmediateWin(game);
        if (win != null) {
            return win;
        }

        List<int[]> lines = getAllLines();

        // Generate all moves
        List<Move> moves = getValidMoves(game);

        //  Defensive filters
        moves = filterImmediateLoss(game, moves);
        moves = filterUnavoidableLoss(game, moves);
        moves = filterAntiFork(game, moves, lines);

        // Safety fallback
        if (moves.isEmpty()) {
            if (game.getWinner() != 0) {
                return new Move(16);
            }

            return new Move(17);
        }


        if (moves.size() == 1) {
            return moves.get(0);
        }

        for (Move move : moves) {
            Game copy = game.deepCopy();
            copy.doMove(move);
            if (copy.isDraw()) {
                if (move.isFirstMove()) {
                    return new Move(17);
                }
                return new Move(17, move.getLocation());
            }
        }

        // Evaluation
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            Game copy = game.deepCopy();
            copy.doMove(move);

            int score = evaluatePosition(copy, lines, move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    /**
     * Getter for all the valid moves in the game.
     * @param game the current game.
     * @return the list of valid moves.
     */

    List<Move> getValidMoves(Game game) {
        List<Move> result = new ArrayList<>();
        Map<Integer, Piece> pieces = game.getAvailablePieces();
        if (game.getCurrentPieceID() == -1) { //Filling the result with only pieces if it is the first move.
            for (int key : pieces.keySet()) {
                result.add(new Move(key));
            }
            return result;
        }

        for (int i = 0; i < 16; i++) { // For all other moves puts a combination of field id and piece id.
            if (game.getBoard().isEmptyField(i)) {
                for (int key : pieces.keySet()) {
                    Move move = new Move(key, i);
                    result.add(move);
                }
            }
        }
        return result;
    }


    /**
     * Finds the move that wins the game immediately, or null if no such move exists.
     * @param game the current game.
     * @return the move that wins or null.
     */

    /*@
        requires game != null;
    @*/

    /**
     * Finds a move that wins the game immediately.
     * If such a move exists, the returned move encodes victory
     * by using piece id 16.
     *
     * @param game current game state
     * @return winning move, or null if none exists
     */
    /*@
    requires game != null;
    @*/
    public Move findImmediateWin(Game game) {
        for (Move move : getValidMoves(game)) {
            Game copy = game.deepCopy();
            copy.doMove(move);
            if (copy.getWinner() == game.getCurrentPlayer()) {
                return move.isFirstMove()
                        ? new Move(16)
                        : new Move(16, move.getLocation());
            }
        }
        return null;
    }


    /**
     * Filters moves in terms of getting rid of moves that immediately lead to loss.
     * @param game the current game.
     * @param moves the list of moves
     * @return the list of moves without moves that lead to loss.
     */
    /**
     * Removes moves that allow the opponent to win immediately.
     */
    private List<Move> filterImmediateLoss(Game game, List<Move> moves) {
        List<Move> safe = new ArrayList<>();

        for (Move move : moves) {
            Game copy = game.deepCopy();
            copy.doMove(move);

            // ENDGAME: opponent has no reply → cannot lose
            if (!hasNextTurn(copy)) {
                safe.add(move);
                continue;
            }

            boolean opponentCanWin = false;

            for (int field : getEmptyFields(copy)) {
                Game sim = copy.deepCopy();
                int nextPiece = getAnyAvailablePiece(sim);
                sim.doMove(new Move(nextPiece, field));

                if (sim.getWinner() != 0) {
                    opponentCanWin = true;
                    break;
                }
            }

            if (!opponentCanWin) {
                safe.add(move);
            }
        }
        return safe;
    }

    /**
     * Filters moves in terms of analyzing,
     * if there is a situation in which there will be a loss no matter what piece is given away.
     * @param game the current game.
     * @param moves moves to analyze.
     * @return the filtered list of moves.
     */
    /**
     * Removes moves that lead to an unavoidable loss,
     * regardless of which piece is given next.
     */
    private List<Move> filterUnavoidableLoss(Game game, List<Move> moves) {
        List<Move> result = new ArrayList<>();

        for (Move move : moves) {
            Game copy = game.deepCopy();
            copy.doMove(move);

            // ENDGAME: no next piece → unavoidable loss impossible
            if (!hasNextTurn(copy)) {
                result.add(move);
                continue;
            }

            boolean unavoidable = false;

            for (int field : getEmptyFields(copy)) {
                boolean losingForAll = true;

                for (int piece : copy.getAvailablePieces().keySet()) {
                    Game sim = copy.deepCopy();
                    sim.doMove(new Move(piece, field));

                    if (sim.getWinner() == 0) {
                        losingForAll = false;
                        break;
                    }
                }

                if (losingForAll) {
                    unavoidable = true;
                    break;
                }
            }

            if (!unavoidable) {
                result.add(move);
            }
        }
        return result;
    }

    /**
     * Evaluates the given game position after a move.
     *
     * @param game the game state AFTER the move
     * @param lines all board lines
     * @param move the move that led to this position
     * @return evaluation score (higher is better)
     */
    /*@
      requires game != null;
      requires lines != null;
      requires move != null;
    @*/
    private int evaluatePosition(Game game, List<int[]> lines, Move move) {

        int score = 0;

        // Win / loss
        if (game.getWinner() != 0) {
            return (game.getWinner() == game.getCurrentPlayer())
                    ? -100000   // opponent just won
                    : 100000;   // we just won
        }

        // Dangerous lines
        int dangerous3 = countDangerousLines(game, lines, 3);
        int dangerous2 = countDangerousLines(game, lines, 2);

        score += dangerous3 * 200;
        score += dangerous2 * 80;

        // Mobility
        int mobility = countSafeMoves(game, lines);
        score += mobility * 5;

        // Geometry (only if move placed a piece)
        if (!move.isFirstMove()) {
            score += geometryScore(move.getLocation());
        }

        return score;
    }

    /**
     * Counts the number of dangerous lines that contain exactly
     * placed pieces sharing at least one common attribute, and for which
     * a continuation piece exists among the available pieces.
     * @param game the current game state
     * @param lines all board lines (rows, columns, diagonals)
     * @param k the number of placed pieces to consider (must be 2 or 3)
     * @return the number of dangerous lines of size
     */
    /*@
      requires game != null;
      requires lines != null;
      requires k == 2 || k == 3;
      ensures \result >= 0;
    @*/
    private int countDangerousLines(Game game, List<int[]> lines, int k) {
        int dangerousCount = 0;

        for (int[] line : lines) {

            // Defensive: ignore malformed lines
            if (line == null || line.length != 4) {
                continue;
            }

            // Collect placed pieces in this line
            Piece[] placed = new Piece[k];
            int placedCount = 0;

            for (int field : line) {
                Piece p = game.getBoard().getField(field);
                if (p != null) {
                    if (placedCount < k) {
                        placed[placedCount] = p;
                    }
                    placedCount++;
                }
            }

            // We only care about lines with exactly k pieces
            if (placedCount != k) {
                continue;
            }

            // Check if the placed pieces share at least one common attribute
            if (!game.getBoard().hasCommonAttribute(placed)) {
                continue;
            }

            // Check if there exists an available piece that can extend this line
            if (existsContinuationPiece(game, placed)) {
                dangerousCount++;
            }
        }

        return dangerousCount;
    }

    /**
     * Getter for all empty fields on a board.
     * @param game the current game.
     * @return the list with all empty fields.
     */
    private List<Integer> getEmptyFields(Game game) {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            if (game.getBoard().isEmptyField(i)) {
                empty.add(i);
            }
        }
        return empty;
    }

    /**
     * Getter for any random piece chosen as nextpiece.
     * @param game is the current game.
     * @return the first available id of a piece.
     */
    private int getAnyAvailablePiece(Game game) {
        for (int pieceId : game.getAvailablePieces().keySet()) {
            return pieceId;
        }
        throw new IllegalStateException("No available pieces");
    }

    /**
     * Returns all lines of a 4x4 Quarto board.
     * A line consists of 4 field indices and represents
     * a row, column, or diagonal.
     * @return a list of all board lines
     */
    /*@
      ensures \result != null;
      ensures \result.size() == 10;
    @*/
    private List<int[]> getAllLines() {
        List<int[]> lines = new ArrayList<>();

        // Rows
        for (int row = 0; row < 4; row++) {
            int[] line = new int[4];
            for (int col = 0; col < 4; col++) {
                line[col] = row * 4 + col;
            }
            lines.add(line);
        }

        // Columns
        for (int col = 0; col < 4; col++) {
            int[] line = new int[4];
            for (int row = 0; row < 4; row++) {
                line[row] = row * 4 + col;
            }
            lines.add(line);
        }

        // Main diagonal
        lines.add(new int[] {0, 5, 10, 15});

        // Anti-diagonal
        lines.add(new int[] {3, 6, 9, 12});

        return lines;
    }

    /**
     * Checks whether there exists an available piece that can extend
     * the given line based on a shared attribute.
     *
     * @param game the current game state
     * @param linePieces the pieces already placed in the line
     * @return true if a continuation piece exists
     */
    /*@
      requires game != null;
      requires linePieces != null;
      requires linePieces.length >= 2;
    @*/
    private boolean existsContinuationPiece(Game game, Piece[] linePieces) {

        // Check SIZE
        if (allSameSize(linePieces)
                && existsAvailablePieceWithSameSize(game, linePieces[0].getSize())) {
            return true;
        }

        // Check SHAPE
        if (allSameShape(linePieces)
                && existsAvailablePieceWithSameShape(game, linePieces[0].getShape())) {
            return true;
        }

        // Check COLOUR
        if (allSameColour(linePieces)
                && existsAvailablePieceWithSameColour(game, linePieces[0].getColour())) {
            return true;
        }

        // Check FILL
        if (allSameFill(linePieces)
                && existsAvailablePieceWithSameFill(game, linePieces[0].getFill())) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether all pieces have the same size.
     * @param pieces array of pieces
     * @return true if all pieces share the same size
     */
    /*@
      requires pieces != null;
      requires pieces.length > 0;
    @*/
    private boolean allSameSize(Piece[] pieces) {
        Size s = pieces[0].getSize();
        for (Piece p : pieces) {
            if (p.getSize() != s) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether all pieces have the same fill.
     * @param pieces array of pieces
     * @return true if all pieces share the same fill
     */
    /*@
      requires pieces != null;
      requires pieces.length > 0;
    @*/
    private boolean allSameFill(Piece[] pieces) {
        Fill f = pieces[0].getFill();
        for (Piece p : pieces) {
            if (p.getFill() != f) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether all pieces have the same shape.
     * @param pieces array of pieces
     * @return true if all pieces share the same shape
     */

    /*@
     requires pieces != null;
     requires pieces.length > 0;
    @*/
    private boolean allSameShape(Piece[] pieces) {
        Shape s = pieces[0].getShape();
        for (Piece p : pieces) {
            if (p.getShape() != s) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether all pieces have the same colour.
     * @param pieces array of pieces
     * @return true if all pieces share the same colour
     */
    /*@
      requires pieces != null;
      requires pieces.length > 0;
    @*/
    private boolean allSameColour(Piece[] pieces) {
        Colour c = pieces[0].getColour();
        for (Piece p : pieces) {
            if (p.getColour() != c) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether an available piece with the given shape exists.
     *
     * @param game the current game
     * @param shape the shape to check
     * @return true if such a piece exists
     */
    /*@
      requires game != null;
      requires shape != null;
    @*/
    private boolean existsAvailablePieceWithSameShape(Game game, Shape shape) {
        for (Piece p : game.getAvailablePieces().values()) {
            if (p.getShape() == shape) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether an available piece with the given size exists.
     *
     * @param game the current game
     * @param size the size to check
     * @return true if such a piece exists
     */
    /*@
      requires game != null;
      requires size != null;
    @*/
    private boolean existsAvailablePieceWithSameSize(Game game, Size size) {
        for (Piece p : game.getAvailablePieces().values()) {
            if (p.getSize() == size) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether an available piece with the given colour exists.
     *
     * @param game the current game
     * @param colour the colour to check
     * @return true if such a piece exists
     */
    /*@
      requires game != null;
      requires colour != null;
    @*/
    private boolean existsAvailablePieceWithSameColour(Game game, Colour colour) {
        for (Piece p : game.getAvailablePieces().values()) {
            if (p.getColour() == colour) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether an available piece with the given fill exists.
     *
     * @param game the current game
     * @param fill the fill to check
     * @return true if such a piece exists
     */
    /*@
      requires game != null;
      requires fill != null;
    @*/
    private boolean existsAvailablePieceWithSameFill(Game game, Fill fill) {
        for (Piece p : game.getAvailablePieces().values()) {
            if (p.getFill() == fill) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all dangerous lines that pass through a given empty field.
     * @param game the current game
     * @param field the field index
     * @param lines all board lines
     * @return list of dangerous lines through the field
     */
    /*@
      requires game != null;
      requires lines != null;
    @*/
    private List<int[]> getDangerousLinesThroughField(
            Game game, int field, List<int[]> lines, int k) {

        List<int[]> result = new ArrayList<>();

        for (int[] line : lines) {

            boolean containsField = false;
            for (int f : line) {
                if (f == field) {
                    containsField = true;
                    break;
                }
            }
            if (!containsField) {
                continue;
            }

            // collect pieces in the line
            Piece[] pieces = new Piece[k];
            int count = 0;

            for (int f : line) {
                Piece p = game.getBoard().getField(f);
                if (p != null) {
                    if (count < k) {
                        pieces[count] = p;
                    }
                    count++;
                }
            }

            if (count == k
                    && game.getBoard().hasCommonAttribute(pieces)
                    && existsContinuationPiece(game, pieces)) {
                result.add(line);
            }
        }

        return result;
    }

    /**
     * Checks whether there exists one available piece that blocks
     * all given dangerous lines.
     * @param game the current game
     * @param dangerousLines list of dangerous lines
     * @return true if a single piece blocks all lines
     */
    /*@
      requires game != null;
      requires dangerousLines != null;
    @*/
    private boolean existsUniversalBlockingPiece(
            Game game, List<int[]> dangerousLines) {

        for (Piece candidate : game.getAvailablePieces().values()) {

            boolean blocksAll = true;

            for (int[] line : dangerousLines) {

                List<Piece> placed = new ArrayList<>();
                for (int f : line) {
                    Piece p = game.getBoard().getField(f);
                    if (p != null) {
                        placed.add(p);
                    }
                }

                Piece[] pieces = placed.toArray(new Piece[0]);

                boolean blocksLine =
                        (allSameSize(pieces)   && candidate.getSize()   == pieces[0].getSize()) ||
                                (allSameShape(pieces)  && candidate.getShape()  == pieces[0].getShape()) ||
                                (allSameColour(pieces) && candidate.getColour() == pieces[0].getColour()) ||
                                (allSameFill(pieces)   && candidate.getFill()   == pieces[0].getFill());

                if (!blocksLine) {
                    blocksAll = false;
                    break;
                }
            }

            if (blocksAll) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a given empty field is a fork position.
     * @param game the current game
     * @param field the field index
     * @param lines all board lines
     * @return true if the field is a fork
     */
    /*@
      requires game != null;
      requires lines != null;
    @*/
    private boolean isForkField(Game game, int field, List<int[]> lines) {

        if (!game.getBoard().isEmptyField(field)) {
            return false;
        }

        List<int[]> dangerousLines =
                getDangerousLinesThroughField(game, field, lines, 2);

        if (dangerousLines.size() < 2) {
            return false;
        }

        // if one piece can block everything → not a fork
        return !existsUniversalBlockingPiece(game, dangerousLines);
    }

    /**
     * Checks whether the opponent has any fork opportunity
     * in the given game state.
     *
     * @param game the game state after a simulated move
     * @param lines all board lines
     * @return true if the opponent can create a fork
     */
    /*@
      requires game != null;
      requires lines != null;
    @*/
    private boolean opponentHasFork(Game game, List<int[]> lines) {
        if (!hasNextTurn(game)) {
            return false;
        }

        for (int field = 0; field < 16; field++) {
            if (game.getBoard().isEmptyField(field)
                    && isForkField(game, field, lines)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Filters out moves that allow the opponent to create a fork
     * in the next turn.
     * @param game the current game
     * @param moves all candidate moves
     * @param lines all board lines
     * @return list of moves that do NOT allow opponent forks
     */
    /*@
      requires game != null;
      requires moves != null;
      requires lines != null;
    @*/
    /**
     * Removes moves that allow the opponent to create a fork.
     */
    private List<Move> filterAntiFork(Game game, List<Move> moves, List<int[]> lines) {
        List<Move> safe = new ArrayList<>();

        for (Move move : moves) {
            Game copy = game.deepCopy();
            copy.doMove(move);

            // ENDGAME → fork impossible
            if (!hasNextTurn(copy)) {
                safe.add(move);
                continue;
            }

            if (!opponentHasFork(copy, lines)) {
                safe.add(move);
            }
        }
        return safe;
    }

    /**
     * Counts the number of safe moves in the given game state.
     * A move is considered safe if it does not lead to immediate loss,
     * unavoidable loss, or allow the opponent to create a fork.
     *
     * @param game the game state to evaluate
     * @param lines all board lines
     * @return number of safe moves
     */
    /*@
      requires game != null;
      requires lines != null;
      ensures \result >= 0;
    @*/
    private int countSafeMoves(Game game, List<int[]> lines) {
        if (!hasNextTurn(game)) {
            return 0;
        }

        List<Move> moves = getValidMoves(game);
        moves = filterImmediateLoss(game, moves);
        moves = filterUnavoidableLoss(game, moves);
        moves = filterAntiFork(game, moves, lines);
        return moves.size();
    }


    /**
     * Computes the mobility score for a given move.
     * The score is defined as the difference between the number of
     * safe moves available to the current player and the opponent
     * after the move is applied.
     *
     * @param game the current game state
     * @param move the move to evaluate
     * @param lines all board lines
     * @return mobility score
     */
    /*@
      requires game != null;
      requires move != null;
      requires lines != null;
    @*/
    private int mobilityScore(Game game, Move move, List<int[]> lines) {
        Game after = game.deepCopy();
        after.doMove(move);

        if (!hasNextTurn(after)) {
            return 0;
        }

        int opponent = countSafeMoves(after, lines);
        int mine = 0;

        for (int piece : after.getAvailablePieces().keySet()) {
            Game sim = after.deepCopy();
            sim.doMove(new Move(piece));
            mine += countSafeMoves(sim, lines);
        }

        mine /= Math.max(1, after.getAvailablePieces().size());
        return mine - opponent;
    }


    /**
     * Selects the move with the best mobility score.
     * @param game the current game state
     * @param moves candidate moves
     * @param lines all board lines
     * @return the move with highest mobility score
     */
    /*@
      requires game != null;
      requires moves != null;
      requires lines != null;
      ensures \result != null;
    @*/
    private Move chooseByMobility(Game game, List<Move> moves, List<int[]> lines) {
        Move bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;
        for (Move move : moves) {
            int score = mobilityScore(game, move, lines);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * Returns a geometric score for a board field.
     * @param field the field index (0–15)
     * @return geometric score
     */
    /*@
      requires field >= 0 && field < 16;
    @*/
    private int geometryScore(int field) {

        // center
        if (field == 5 || field == 6 || field == 9 || field == 10) {
            return 3;
        }

        // corners
        if (field == 0 || field == 3 || field == 12 || field == 15) {
            return 2;
        }

        // edges
        return 1;
    }

    /**
     * Chooses the move with the best geometric position.
     *
     * @param moves candidate moves
     * @return move with highest geometry score
     */
    /*@
      requires moves != null;
      requires moves.size() > 0;
    @*/
    private Move chooseByGeometry(List<Move> moves) {

        Move best = moves.get(0);
        int bestScore = -1;

        for (Move move : moves) {

            // first move has no location
            if (move.isFirstMove()) {
                continue;
            }

            int score = geometryScore(move.getLocation());
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return best;
    }

    /**
     * Checks whether the game has a possible next turn
     * (i.e. there are still pieces to give).
     * @param game current game state
     * @return true if at least one piece is still available
     */

    /*@
       requires game != null;
       ensures \result == (!game.getAvailablePieces().isEmpty());
     @*/
    private boolean hasNextTurn(Game game) {
        return !game.getAvailablePieces().isEmpty();
    }

}
