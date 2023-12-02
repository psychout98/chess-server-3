package com.example.chessserver3.service;

import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.model.board.AnalysisBoard;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Move;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@EnableAsync
public class AutoMoveService {


    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private SimpMessagingTemplate template;

    @Async
    public void autoMove(final Board board) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//        ForkJoinPool commonPool = ForkJoinPool.commonPool();
//        AnalysisBoard analysisBoard = new AnalysisBoard(board, 1, 0, 0);
//        Board bestBoard = commonPool.invoke(analysisBoard);
//        String moveCode = moves;
        List<Move> moves = board.getMoves().values().stream().filter(Move::isValid).toList();
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        AnalysisBoard analysisBoard = new AnalysisBoard(moves, 1, board.getBoardKeyString(), board.isWhiteToMove());
        Move bestMove = commonPool.invoke(analysisBoard);
        board.move(bestMove.getMoveCode());
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
        //Thread.currentThread().interrupt();
    }

    public Move findBestMove(List<Move> moves, int depth, String boardKeyString, boolean whiteToMove) {
        Move bestMove;
        Optional<Move> randomMove = moves.stream().findAny();
        if (randomMove.isPresent()) {
            bestMove = randomMove.get();
        } else {
            throw new InvalidMoveException("No moves");
        }
        if (depth > 0) {
            for (Move move : moves) {
                List<Move> futureMoves = move.validate(boardKeyString, whiteToMove, false).stream().filter(Move::isValid).toList();
                Move bestFutureMove = findBestMove(futureMoves, depth - 1, move.getBoardKeyString(), !move.isWhite());
                if (whiteToMove) {
                    if (bestFutureMove.getAdvantage() > bestMove.getAdvantage()) {
                        move.setAdvantage(bestFutureMove.getAdvantage());
                    }
                } else {
                    if (bestFutureMove.getAdvantage() < bestMove.getAdvantage()) {
                        move.setAdvantage(bestFutureMove.getAdvantage());
                    }
                }
            }
        }
        for (Move move : moves) {
            if (whiteToMove) {
                if (move.getAdvantage() > bestMove.getAdvantage()) {
//                    System.out.println(bestMove.getAdvantage() + " " + move.getAdvantage() + " (white)");
                    bestMove = move;
                }
            } else {
                if (move.getAdvantage() < bestMove.getAdvantage()) {
//                    System.out.println(bestMove.getAdvantage() + " " + move.getAdvantage() + " (black)");
                    bestMove = move;
                }
            }
        }
//        System.out.println(bestMove.getAdvantage() + " " + depth);
        return bestMove;
    }

}
