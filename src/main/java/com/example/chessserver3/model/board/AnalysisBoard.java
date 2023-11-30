package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import lombok.Getter;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class AnalysisBoard extends Thread {

    private final Board board;
    private final int depth;
    private Board bestBoard;

    public AnalysisBoard(Board board, int depth) {
        this.board = board;
        this.depth = depth;
    }

    public void run() {
        bestBoard = board.copy(false, board.getCurrentMove());
        Set<Move> moves = board.getPieces().values().stream().filter(piece -> piece.isWhite() == board.isWhiteToMove()).map(Piece::getMoves).flatMap(Set::stream).collect(Collectors.toSet());
        Optional<Move> randomMove = moves.stream().findAny();
        if (randomMove.isPresent()) {
            bestBoard.move(randomMove.get().getMoveCode(), bestBoard.isWhiteToMove());
        } else {
            throw new InvalidMoveException("No moves");
        }
        if (depth > 1) {
            int highestAdvantage = 0;
            Set<FutureBoard> futureBoards = moves.stream().map(move -> new FutureBoard(board, false, move)).collect(Collectors.toSet());
            futureBoards.forEach(FutureBoard::start);
            futureBoards.forEach(futureBoard -> {
                try {
                    futureBoard.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            futureBoards.forEach(FutureBoard::interrupt);
            Set<AnalysisBoard> analysisBoards = futureBoards.stream().map(futureBoard -> new AnalysisBoard(futureBoard.getBoard(), depth - 1)).collect(Collectors.toSet());
            analysisBoards.forEach(AnalysisBoard::start);
            analysisBoards.forEach(analysisBoard -> {
                try {
                    analysisBoard.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            analysisBoards.forEach(AnalysisBoard::interrupt);
            for (AnalysisBoard analysisBoard : analysisBoards) {
                int advantage = analysisBoard.bestBoard.getAdvantage();
                if ((analysisBoard.bestBoard.isWhiteToMove() && advantage > highestAdvantage) || (!analysisBoard.bestBoard.isWhiteToMove() && advantage < highestAdvantage)) {
                    highestAdvantage = advantage;
                    bestBoard = analysisBoard.getBoard();
                }
            }
        }
    }
}
