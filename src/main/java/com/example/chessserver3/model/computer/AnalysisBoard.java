package com.example.chessserver3.model.computer;

import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.exception.MoveException;
import lombok.*;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Getter
public class AnalysisBoard extends RecursiveAction {

    private final BoardData boardData;
    private boolean valid;
    private final byte depth;
    private final byte maxDepth;
    private final String lastMoveCode;
    private String bestMoveCode;
    private int advantage;
//    private long possibleMoves;
//    private long evaluatedMoves;

    public AnalysisBoard(BoardData boardData, byte depth, byte maxDepth, String lastMoveCode) {
        this.boardData = boardData;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.lastMoveCode = lastMoveCode;
        bestMoveCode = "resign";
        advantage = boardData.getMaterialAdvantage();
        valid = true;
//        possibleMoves = 0;
//        evaluatedMoves = 0;
    }

    @Override
    public void compute() {
        Collection<AnalysisBoard> futures = new ArrayList<>();
        for (byte i=0;i<8;i++) {
            for (byte j=0;j<8;j++) {
                char key = boardData.keyAtSpace(i, j);
                if (key != 'x' &&
                        (boardData.isWhiteToMove() ?
                                Character.isUpperCase(key) :
                                Character.isLowerCase(key))) {
                    String pieceKey = String.format("%c%x%x", key, i, j);
                    List<MoveNode> moveNodes = Moves.moves.get(pieceKey);
                    for (MoveNode moveNode : moveNodes) {
                        while (moveNode != null) {
//                            possibleMoves++;
                            try {
                                ComputerMove computerMove = new ComputerMove(moveNode.getMoveArray(), boardData, depth, maxDepth);
                                if (computerMove.getEndKey() == 'k' || computerMove.getEndKey() == 'K') {
                                    valid = false;
                                    break;
                                } else if (computerMove.getEndKey() != 'x') {
                                    moveNode = null;
                                } else {
                                    moveNode = moveNode.getNext();
                                }
                                futures.add(computerMove.getAnalysisBoard());
                            } catch (InvalidMoveException e) {
                                if (e.getMoveException() == MoveException.OBSTRUCTED_PATH) {
                                    moveNode = null;
                                } else {
                                    moveNode = moveNode != null ? moveNode.getNext() : null;
                                }
                            }
                        }
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
                AnalysisBoard bestFuture = boardData.isWhiteToMove() ? Collections.max(futures, AdvantageComparator.comparator) : Collections.min(futures, AdvantageComparator.comparator);
                bestMoveCode = bestFuture.lastMoveCode;
                advantage = bestFuture.getAdvantage();
            } catch (NoSuchElementException ignored) {
                bestMoveCode = "resign";
                advantage = boardData.isWhiteToMove() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            }
//            evaluatedMoves = futures.size() + futures.stream().mapToLong(AnalysisBoard::getEvaluatedMoves).sum();
//            possibleMoves += futures.stream().mapToLong(AnalysisBoard::getPossibleMoves).sum();
        }
    }
}
