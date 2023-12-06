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

import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;

@Service
@EnableAsync
public class AutoMoveService {


    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private SimpMessagingTemplate template;

    @Async
    public void autoMove(final Board board, final int depth) {
        board.move(findBestMove(board, depth).getMoveCode());
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
    }

    private Move findBestMove(final Board board, final int depth) {
        if (board.getHistory().size() < 2) {
            throw new InvalidMoveException("Cannot generate futures before move 2");
        }
        Move twoMovesAgo = board.getHistory().get(board.getHistory().size() - 2);
        Move previousMove = board.getHistory().get(board.getHistory().size() - 1);
        AnalysisBoard analysisBoard = AnalysisBoard.builder()
                .depth(depth)
                .oldBoardKeyString(twoMovesAgo.getBoardKeyString())
                .previousMove(previousMove)
                .root(true)
                .build();
        analysisBoard.generateFutures();
        if (analysisBoard.getPreviousMove().isValid()) {
            ForkJoinPool commonPool = ForkJoinPool.commonPool();
            return commonPool.invoke(analysisBoard);
        } else {
            throw new InvalidMoveException("Invalid base move " + analysisBoard.getPreviousMove().getMoveString());
        }
    }

}
