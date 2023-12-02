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
import java.util.concurrent.ForkJoinPool;

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
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Move> moves = board.getMoves().values().stream().filter(Move::isValid).toList();
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        AnalysisBoard analysisBoard = new AnalysisBoard(moves, 1, board.getBoardKeyString(), board.isWhiteToMove());
        Move bestMove = commonPool.invoke(analysisBoard);
        board.move(bestMove.getMoveCode());
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
    }

}
