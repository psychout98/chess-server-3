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

//    private final Board board;
//    private final int depth;
//    private Board bestBoard;
//    private int highestAdvantageWhite, highestAdvantageBlack;
//
//    public AnalysisBoard(Board board, int depth, int highestAdvantageWhite, int highestAdvantageBlack) {
//        this.board = board;
//        this.depth = depth;
//        this.bestBoard = board.copy(false, board.getCurrentMove());
//        this.highestAdvantageWhite = highestAdvantageWhite;
//        this.highestAdvantageBlack = highestAdvantageBlack;
//    }
//
//    @Override
//    protected Board compute() {
//        if (!board.isCheckmate()) {
//            Set<String> moves = board.getPieces().values().stream()
//                    .filter(piece -> piece.isWhite() == board.isWhiteToMove()).map(Piece::getMoves)
//                    .flatMap(Set::stream).map(Move::getMoveCode).collect(Collectors.toSet());
//            Optional<String> randomMove = moves.stream().findAny();
//            if (randomMove.isPresent()) {
//                bestBoard.move(randomMove.get(), board.isWhiteToMove());
//            } else {
//                throw new InvalidMoveException("No moves");
//            }
//            if (depth > 0) {
//                Set<Board> futureBoards = ForkJoinTask.invokeAll(createFutureBoards(moves))
//                        .stream()
//                        .map(ForkJoinTask::join)
//                        .collect(Collectors.toSet());
//                for (Board futureBoard : futureBoards) {
//                    int advantage = futureBoard.calculateAdvantage();
//                    if (advantage < highestAdvantageBlack && !futureBoard.isWhiteToMove()) {
////                        System.out.println("\n" + advantage);
////                        System.out.println(futureBoard.getHistory().stream().map(Move::getMoveString).toList());
//                        highestAdvantageBlack = advantage;
//                        bestBoard = futureBoard;
//                    }
//                    if (advantage > highestAdvantageBlack && futureBoard.isWhiteToMove()) {
////                        System.out.println("\n" + advantage);
////                        System.out.println(futureBoard.getHistory().stream().map(Move::getMoveString).toList());
//                        highestAdvantageBlack = advantage;
//                        bestBoard = futureBoard;
//                    }
//                }
//            }
//        }
//        return bestBoard;
//    }
//
//    private Collection<AnalysisBoard> createFutureBoards(Set<String> moves) {
//        return moves.stream().map(move -> {
//            Board deepCopy = board.copy(false, board.getCurrentMove());
//            deepCopy.move(move, board.isWhiteToMove());
//            return new AnalysisBoard(deepCopy, depth - 1, highestAdvantageWhite, highestAdvantageBlack);
//        }).collect(Collectors.toList());
//    }
}
