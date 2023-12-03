package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class AnalysisBoard extends RecursiveTask<Move> {

    private Move lastMove;
    private Map<String, Move> moves;
    private int depth;
    private String boardKeyString;
    private boolean whiteToMove;


    @Override
    protected Move compute() {
        Move bestMove = lastMove;
        if (depth > 0) {
            Collection<AnalysisBoard> analysisBoards = new ArrayList<>();
            for (Move move : moves.values()) {
                 Map<String, Move> futureMoves = move.validate(boardKeyString, whiteToMove, false)
                         .stream().filter(Move::isValid)
                         .collect(Collectors.toMap(Move::getMoveCode, Function.identity()));
                analysisBoards.add(new AnalysisBoard(move, futureMoves, depth - 1, move.getBoardKeyString(), !move.isWhite()));
            }
            List<Move> futureMoves = ForkJoinTask.invokeAll(analysisBoards)
                    .stream()
                    .map(ForkJoinTask::join)
                    .toList();
            for (Move move : futureMoves) {
                Move originalMove = moves.get(move.getLastMove().getMoveCode());
                if (originalMove != null) {
                    originalMove.setAdvantage(move.getAdvantage());
                }
            }
        }
        Optional<Move> randomMove = moves.values().stream().findAny();
        if (randomMove.isPresent()) {
            bestMove = randomMove.get();
        }
        for (Move move : moves.values()) {
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