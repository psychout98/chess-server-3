package com.example.chessserver3.service;

import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.AnalysisBoard;
import com.example.chessserver3.model.board.Move;
import com.example.chessserver3.model.board.Piece;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@EnableAsync
public class AutoMoveService {


    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private SimpMessagingTemplate template;

    @Async
    public void autoMove(Board board, boolean white) throws InterruptedException {
        AnalysisBoard analysisBoard = new AnalysisBoard(board, 1);
        analysisBoard.start();
        analysisBoard.join();
        analysisBoard.interrupt();
        Board bestBoard = analysisBoard.getBestBoard();
        board.move(bestBoard.getHistory().get(bestBoard.getHistory().size() - 1).getMoveCode(), white);
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
        Thread.currentThread().interrupt();
    }

}
