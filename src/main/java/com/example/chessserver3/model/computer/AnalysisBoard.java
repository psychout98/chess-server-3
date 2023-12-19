package com.example.chessserver3.model.computer;

import com.example.chessserver3.exception.InvalidMoveException;
import lombok.*;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static com.example.chessserver3.model.board.Move.pointValues;

@Getter
public class AnalysisBoard extends RecursiveAction {

    private final ShortFEN shortFen;
    private boolean valid;
    private final byte depth;
    private final byte maxDepth;
    private final String lastMoveCode;
    private String bestMoveCode;
    private int advantage;
//    private int count;

    public AnalysisBoard(ShortFEN shortFEN, byte depth, byte maxDepth, String lastMoveCode) {
        this.shortFen = shortFEN;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.lastMoveCode = lastMoveCode;
        this.bestMoveCode = "resign";
        this.advantage = calculateAdvantage(shortFEN.getBoardCode());
        this.valid = true;
//        this.count = 0;
    }

    private static int calculateAdvantage(String boardCode) {
        int materialAdvantage = 0;
        for (char key : boardCode.toCharArray()) {
            materialAdvantage += calculatePoints(key);
        }
        return materialAdvantage;
    }

    private static int calculatePoints(char key) {
        return Objects.requireNonNullElse(pointValues.get(key), 0);
    }

    @Override
    public void compute() {
        Collection<AnalysisBoard> futures = new ArrayList<>();
        for (byte i=0;i<8;i++) {
            for (byte j=0;j<8;j++) {
                if (shortFen.getBoardKey()[i][j] != 'x') {
                    String pieceKey = String.format("%c%x%x", shortFen.getBoardKey()[i][j], i, j);
                    List<byte[]> pieceMoves = Pieces.moves.get(pieceKey);
                    for (byte[] moveArray : pieceMoves) {
                        try {
                            ComputerMove computerMove = new ComputerMove(moveArray, shortFen, depth, maxDepth);
                            if (computerMove.isKingKiller()) {
                                valid = false;
                                break;
                            }
                            futures.add(computerMove.getAnalysisBoard());
                        } catch (InvalidMoveException ignored) {}
                    }
                }
            }
        }
        if (valid) {
            if (depth < maxDepth) {
                ForkJoinTask.invokeAll(futures);
            }
            futures.removeIf(future -> !future.valid);
            try {
                AnalysisBoard bestFuture = shortFen.isWhiteToMove() ? Collections.max(futures, BoardComparator.comparator) : Collections.min(futures, BoardComparator.comparator);
                bestMoveCode = bestFuture.lastMoveCode;
                advantage = bestFuture.getAdvantage();
            } catch (NoSuchElementException ignored) {
                bestMoveCode = "resign";
                advantage = shortFen.isWhiteToMove() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            }
//            count = futures.size() + futures.stream().mapToInt(AnalysisBoard::getCount).sum();
        }
    }
}
