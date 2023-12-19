package com.example.chessserver3.service;

import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.computer.AnalysisBoard;
import com.example.chessserver3.model.computer.BoardData;
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
            BoardData boardData = new BoardData(board.getFen().getFen());
            AnalysisBoard analysisBoard = new AnalysisBoard(boardData, (byte) 0, level, board.getLastMoveCode());
            ForkJoinPool commonPool = ForkJoinPool.commonPool();
//            long startTime = System.nanoTime();
            commonPool.invoke(analysisBoard);
//            long endTime = System.nanoTime();
//            System.out.println(analysisBoard.getPossibleMoves() + " moves checked");
//            System.out.println(1000000000 * analysisBoard.getEvaluatedMoves() / (endTime - startTime) + " moves per second");
            board.move(analysisBoard.getBestMoveCode());
            boardRepository.update(board);
            template.convertAndSend("/board/" + board.getId(), "computer");
        }
    }

}