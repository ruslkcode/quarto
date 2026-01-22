package client;

import gameLogic.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Smart AI Strategy with LAST MOVE FIX.
 */
public class SmartStrategy implements BotStrategy {

    private static final long TIME_LIMIT = 800;
    private long startTime;

    @Override
    public String getName() {
        return "Smart (Final v2)";
    }

    @Override
    public Move determineMove(Game game) {
        startTime = System.currentTimeMillis();

        Move win = findImmediateWin(game);
        if (win != null) return win;

        List<int[]> lines = getAllLines();
        List<Move> allMoves = getValidMoves(game);

        if (allMoves.isEmpty()) return null;

        // Defensive Filters
        List<Move> moves = filterImmediateLoss(game, allMoves);
        if (moves.isEmpty()) moves = allMoves;

        if (!isTimeUp()) {
            List<Move> betterMoves = filterUnavoidableLoss(game, moves);
            if (!betterMoves.isEmpty()) moves = betterMoves;
        }

        if (!isTimeUp()) {
            List<Move> antiForkMoves = filterAntiFork(game, moves, lines);
            if (!antiForkMoves.isEmpty()) moves = antiForkMoves;
        }

        if (moves.isEmpty()) return allMoves.get(0);

        Move bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            if (isTimeUp()) break;

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

    private boolean isTimeUp() {
        return (System.currentTimeMillis() - startTime) > TIME_LIMIT;
    }

    // --- ИСПРАВЛЕНИЕ ЗДЕСЬ ---
    List<Move> getValidMoves(Game game) {
        List<Move> result = new ArrayList<>();
        Map<Integer, Piece> pieces = game.getAvailablePieces();

        // 1. First move
        if (game.getCurrentPieceID() == -1) {
            for (int key : pieces.keySet()) result.add(new Move(key));
            return result;
        }

        // 2. ПОСЛЕДНИЙ ХОД (Last Move Fix)
        if (pieces.isEmpty()) {
            for (int i = 0; i < 16; i++) {
                if (game.getBoard().isEmptyField(i)) {
                    // Ставим -1 вместо 0!
                    // 0 - это реальная фигура, которая занята. -1 значит "ничего".
                    result.add(new Move(-1, i));
                }
            }
            return result;
        }

        // 3. Normal move
        for (int i = 0; i < 16; i++) {
            if (game.getBoard().isEmptyField(i)) {
                for (int key : pieces.keySet()) {
                    result.add(new Move(key, i));
                }
            }
        }
        return result;
    }

    // --- ОСТАЛЬНОЕ БЕЗ ИЗМЕНЕНИЙ ---

    public Move findImmediateWin(Game game) {
        if (game.getCurrentPieceID() == -1) return null;
        for (int i = 0; i < 16; i++) {
            if (game.getBoard().isEmptyField(i)) {
                Move move = new Move(0, i);
                Game copy = game.deepCopy();
                copy.doMove(move);
                if (copy.getWinner() == game.getCurrentPlayer()) {
                    if (!game.getAvailablePieces().isEmpty()) {
                        int nextPiece = game.getAvailablePieces().keySet().iterator().next();
                        return new Move(nextPiece, i);
                    } else return new Move(-1, i); // Fix here too
                }
            }
        }
        return null;
    }

    private List<Move> filterImmediateLoss(Game game, List<Move> moves) {
        ArrayList<Move> safe = new ArrayList<>();
        for (Move move : moves) {
            if (isTimeUp() && !safe.isEmpty()) return safe;
            Game copy = game.deepCopy();
            copy.doMove(move);
            boolean opponentCanWin = false;

            List<Integer> emptyFields = getEmptyFields(copy);
            if (!emptyFields.isEmpty()) {
                for (int field : emptyFields) {
                    Game simulation = copy.deepCopy();
                    simulation.doMove(new Move(0, field));
                    if (simulation.getWinner() != 0) {
                        opponentCanWin = true;
                        break;
                    }
                }
            }
            if (!opponentCanWin) safe.add(move);
        }
        return safe;
    }

    private List<Move> filterUnavoidableLoss(Game game, List<Move> moves) {
        ArrayList<Move> result = new ArrayList<>();
        for (Move move : moves) {
            if (isTimeUp() && !result.isEmpty()) return result;
            Game copy = game.deepCopy();
            copy.doMove(move);
            boolean unavoidable = false;
            List<Integer> emptyFields = getEmptyFields(copy);
            if (emptyFields.isEmpty()) { result.add(move); continue; }

            for (int field : emptyFields) {
                boolean losingForAllPieces = true;
                Map<Integer, Piece> avail = copy.getAvailablePieces();
                if (avail.isEmpty()) { losingForAllPieces = false; }

                for (int piece : avail.keySet()) {
                    Game simulation = copy.deepCopy();
                    simulation.doMove(new Move(piece, field));
                    if (simulation.getWinner() == 0) { losingForAllPieces = false; break; }
                }
                if (losingForAllPieces) { unavoidable = true; break; }
            }
            if (!unavoidable) result.add(move);
        }
        return result;
    }

    private int evaluatePosition(Game game, List<int[]> lines, Move move) {
        if (game.getWinner() != 0) return (game.getWinner() == game.getCurrentPlayer()) ? 100000 : -100000;
        int score = countDangerousLines(game, lines, 3) * 200 + countDangerousLines(game, lines, 2) * 80;
        if (!isTimeUp()) score += countSafeMoves(game, lines) * 5;
        if (!move.isFirstMove()) score += geometryScore(move.getLocation());
        return score;
    }

    private int countDangerousLines(Game game, List<int[]> lines, int k) {
        int dangerousCount = 0;
        for (int[] line : lines) {
            Piece[] placed = new Piece[k];
            int placedCount = 0;
            for (int field : line) {
                Piece p = game.getBoard().getField(field);
                if (p != null) { if (placedCount < k) placed[placedCount] = p; placedCount++; }
            }
            if (placedCount == k && game.getBoard().hasCommonAttribute(placed) && existsContinuationPiece(game, placed)) dangerousCount++;
        }
        return dangerousCount;
    }

    private List<Integer> getEmptyFields(Game game) {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < 16; i++) if (game.getBoard().isEmptyField(i)) empty.add(i);
        return empty;
    }
    private List<int[]> getAllLines() {
        List<int[]> lines = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int[] r = new int[4]; int[] c = new int[4];
            for (int j = 0; j < 4; j++) { r[j] = i*4+j; c[j] = j*4+i; }
            lines.add(r); lines.add(c);
        }
        lines.add(new int[]{0,5,10,15}); lines.add(new int[]{3,6,9,12});
        return lines;
    }
    private boolean existsContinuationPiece(Game game, Piece[] lp) {
        if (allSameSize(lp) && hasP(game, lp[0].getSize())) return true;
        if (allSameShape(lp) && hasP(game, lp[0].getShape())) return true;
        if (allSameColor(lp) && hasP(game, lp[0].getColour())) return true;
        if (allSameFill(lp) && hasP(game, lp[0].getFill())) return true;
        return false;
    }
    private boolean allSameSize(Piece[] p) { Size s = p[0].getSize(); for(Piece x:p) if(x.getSize()!=s) return false; return true; }
    private boolean allSameFill(Piece[] p) { Fill f = p[0].getFill(); for(Piece x:p) if(x.getFill()!=f) return false; return true; }
    private boolean allSameShape(Piece[] p) { Shape s = p[0].getShape(); for(Piece x:p) if(x.getShape()!=s) return false; return true; }
    private boolean allSameColor(Piece[] p) { Colour c = p[0].getColour(); for(Piece x:p) if(x.getColour()!=c) return false; return true; }
    private boolean hasP(Game g, Size s) { for(Piece p:g.getAvailablePieces().values()) if(p.getSize()==s) return true; return false; }
    private boolean hasP(Game g, Shape s) { for(Piece p:g.getAvailablePieces().values()) if(p.getShape()==s) return true; return false; }
    private boolean hasP(Game g, Colour c) { for(Piece p:g.getAvailablePieces().values()) if(p.getColour()==c) return true; return false; }
    private boolean hasP(Game g, Fill f) { for(Piece p:g.getAvailablePieces().values()) if(p.getFill()==f) return true; return false; }

    private List<int[]> getDangerousLinesThroughField(Game game, int field, List<int[]> lines, int k) {
        List<int[]> result = new ArrayList<>();
        for (int[] line : lines) {
            boolean hasF = false; for(int f:line) if(f==field) hasF=true; if(!hasF) continue;
            Piece[] p = new Piece[k]; int c=0;
            for(int f:line) { Piece pc=game.getBoard().getField(f); if(pc!=null && c<k) p[c++]=pc; }
            if(c==k && game.getBoard().hasCommonAttribute(p) && existsContinuationPiece(game, p)) result.add(line);
        }
        return result;
    }
    private boolean existsUniversalBlockingPiece(Game game, List<int[]> dLines) {
        for(Piece cand:game.getAvailablePieces().values()) {
            boolean blocksAll = true;
            for(int[] line:dLines) {
                List<Piece> pl = new ArrayList<>(); for(int f:line) if(game.getBoard().getField(f)!=null) pl.add(game.getBoard().getField(f));
                Piece[] p = pl.toArray(new Piece[0]);
                boolean blocks = (allSameSize(p) && cand.getSize()==p[0].getSize()) ||
                        (allSameShape(p) && cand.getShape()==p[0].getShape()) ||
                        (allSameColor(p) && cand.getColour()==p[0].getColour()) ||
                        (allSameFill(p) && cand.getFill()==p[0].getFill());
                if(!blocks) { blocksAll=false; break; }
            }
            if(blocksAll) return true;
        }
        return false;
    }
    private List<Move> filterAntiFork(Game game, List<Move> moves, List<int[]> lines) {
        List<Move> safe = new ArrayList<>();
        for(Move m:moves) {
            if(isTimeUp() && !safe.isEmpty()) return safe;
            Game c = game.deepCopy(); c.doMove(m);
            boolean fork = false;
            for(int f=0;f<16;f++) {
                if(c.getBoard().isEmptyField(f)) {
                    List<int[]> dl = getDangerousLinesThroughField(c, f, lines, 2);
                    if(dl.size()>=2 && !existsUniversalBlockingPiece(c, dl)) { fork=true; break; }
                }
            }
            if(!fork) safe.add(m);
        }
        return safe;
    }
    private int countSafeMoves(Game game, List<int[]> lines) {
        return filterImmediateLoss(game, getValidMoves(game)).size();
    }
    private int geometryScore(int f) {
        if(f==5||f==6||f==9||f==10) return 3;
        if(f==0||f==3||f==12||f==15) return 2;
        return 1;
    }
}