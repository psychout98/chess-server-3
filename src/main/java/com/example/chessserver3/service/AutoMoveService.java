package com.example.chessserver3.service;

import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Move;
import com.example.chessserver3.model.computer.AnalysisBoard;
import com.example.chessserver3.model.computer.ShortFEN;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.concurrent.ForkJoinPool;


@Service
@EnableAsync
public class AutoMoveService {


    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private SimpMessagingTemplate template;

    @Async
    public void autoMove(final Board board, final byte level) {
        if (board.getWinner() == 0) {
            ShortFEN shortFEN = new ShortFEN(board.getFen().getFen());
            AnalysisBoard analysisBoard = new AnalysisBoard(shortFEN, (byte) 0, level, board.getLastMoveCode());
            ForkJoinPool commonPool = ForkJoinPool.commonPool();
            commonPool.invoke(analysisBoard);
            board.move(analysisBoard.getBestMoveCode());
            boardRepository.update(board);
            template.convertAndSend("/board/" + board.getId(), "computer");
        }
    }

}