package com.example.chessserver3.service;

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
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@EnableAsync
public class AutoMoveService {


    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private SimpMessagingTemplate template;

    @Async
    public void autoMove(final Board board, int attempt) {
        try {
            Map<String, Move> moves = board.getMoves().values().stream().filter(Move::isValid)
                    .collect(Collectors.toMap(Move::getMoveCode, Function.identity()));
            ForkJoinPool commonPool = ForkJoinPool.commonPool();
            AnalysisBoard analysisBoard = new AnalysisBoard(board.getHistory().get(board.getHistory().size() - 1), moves, 2, board.getBoardKeyString(), board.isWhiteToMove());
            Move bestMove = commonPool.invoke(analysisBoard);
            board.move(bestMove.getMoveCode());
        } catch (Exception e) {
            if (attempt < 5) {
                autoMove(board, attempt + 1);
            } else {
                board.resign(board.isWhiteToMove());
            }
        }
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
    }

}
