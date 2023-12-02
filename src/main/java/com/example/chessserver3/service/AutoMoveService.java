package com.example.chessserver3.service;

import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Move;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        Optional<Move> move = board.getMoves().values().stream().filter(Move::isValid).findAny();
        if (move.isPresent()) {
            board.move(move.get().getMoveCode());
            boardRepository.update(board);
            template.convertAndSend("/board/" + board.getId(), "update");
        }
        //Thread.currentThread().interrupt();
    }

}
