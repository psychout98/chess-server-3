package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import lombok.Getter;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Getter
public class AnalysisBoard extends RecursiveTask<Board> {

    private final Board board;
    private final int depth;
    private Board bestBoard;
    private int highestAdvantageWhite, highestAdvantageBlack;

    public AnalysisBoard(Board board, int depth) {
        this.board = board;
        this.depth = depth;
        this.bestBoard = board.copy(false);
        this.highestAdvantageWhite = board.calculateAdvantage();
        this.highestAdvantageBlack = this.highestAdvantageWhite;
    }

    @Override
    protected Board compute() {
        if (!board.isCheckmate()) {
            Set<String> moves = board.getPieces().values().stream()
                    .filter(piece -> piece.isWhite() == board.isWhiteToMove()).map(Piece::getMoves)
                    .flatMap(Set::stream).map(Move::getMoveCode).collect(Collectors.toSet());
            Optional<String> randomMove = moves.stream().findAny();
            if (randomMove.isPresent()) {
                bestBoard.move(randomMove.get(), board.isWhiteToMove());
            } else {
                throw new InvalidMoveException("No moves");
            }
            if (depth > 0) {
                Set<Board> futureBoards = ForkJoinTask.invokeAll(createFutureBoards(moves))
                        .stream()
                        .map(ForkJoinTask::join)
                        .collect(Collectors.toSet());
                for (Board futureBoard : futureBoards) {
                    int advantage = futureBoard.calculateAdvantage();
                    if (advantage < highestAdvantageBlack && !board.isWhiteToMove()) {
//                        System.out.println("\n" + advantage);
//                        System.out.println(futureBoard.getHistory().stream().map(Move::getMoveString).toList());
//                        System.out.println(highestAdvantageWhite + ", " + highestAdvantageBlack + ", " + advantage + ", " + futureBoard.isWhiteToMove());
                        highestAdvantageBlack = advantage;
                        bestBoard = futureBoard;
                    }
                    if (advantage > highestAdvantageWhite && board.isWhiteToMove()) {
//                        System.out.println("\n" + advantage);
//                        System.out.println(futureBoard.getHistory().stream().map(Move::getMoveString).toList());
//                        System.out.println(highestAdvantageWhite + ", " + highestAdvantageBlack + ", " + advantage + ", " + futureBoard.isWhiteToMove());
                        highestAdvantageWhite = advantage;
                        bestBoard = futureBoard;
                    }
                }
            }
        }
        return bestBoard;
    }

    private Collection<AnalysisBoard> createFutureBoards(Set<String> moves) {
        return moves.stream().map(move -> {
            Board deepCopy = board.copy(false);
            deepCopy.move(move, board.isWhiteToMove());
            return new AnalysisBoard(deepCopy, depth - 1);
        }).collect(Collectors.toList());
    }
}
