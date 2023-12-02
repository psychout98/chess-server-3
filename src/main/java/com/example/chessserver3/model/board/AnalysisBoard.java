package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class AnalysisBoard extends RecursiveTask<Move> {

    private List<Move> moves;
    private int depth;
    private String boardKeyString;
    private boolean whiteToMove;

    @Override
    protected Move compute() {
        Move bestMove;
        Optional<Move> randomMove = moves.stream().findAny();
        if (randomMove.isPresent()) {
            bestMove = randomMove.get();
        } else {
            throw new InvalidMoveException("No moves");
        }
        if (depth > 0) {
            Collection<AnalysisBoard> analysisBoards = new ArrayList<>();
            for (Move move : moves) {
                List<Move> futureMoves = move.validate(boardKeyString, whiteToMove, false).stream().filter(Move::isValid).toList();
                analysisBoards.add(new AnalysisBoard(futureMoves, depth - 1, move.getBoardKeyString(), !move.isWhite()));
            }
            Set<Move> futureMoves = ForkJoinTask.invokeAll(analysisBoards)
                    .stream()
                    .map(ForkJoinTask::join)
                    .collect(Collectors.toSet());
            for (Move move : futureMoves) {
                if (whiteToMove) {
                    if (move.getAdvantage() > bestMove.getAdvantage()) {
                        move.setAdvantage(move.getAdvantage());
                    }
                } else {
                    if (move.getAdvantage() < bestMove.getAdvantage()) {
                        move.setAdvantage(move.getAdvantage());
                    }
                }
            }
        }
        for (Move move : moves) {
            if (whiteToMove) {
                if (move.getAdvantage() > bestMove.getAdvantage()) {
                    bestMove = move;
                }
            } else {
                if (move.getAdvantage() < bestMove.getAdvantage()) {
                    bestMove = move;
                }
            }
        }
        return bestMove;
    }
}