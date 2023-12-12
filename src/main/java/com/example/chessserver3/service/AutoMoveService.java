package com.example.chessserver3.service;

import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Move;
import com.example.chessserver3.model.board.TreeView;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.Arrays;


@Service
@EnableAsync
public class AutoMoveService {


    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private SimpMessagingTemplate template;

    @Async
    public void autoMove(final Board board, final int depth) {
        if (board.getWinner() == 0) {
            Move currentMove = board.getLastMove();
            try {
                board.move(currentMove.findBestFuture(depth).getMoveCode());
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println(Arrays.stream(e.getStackTrace()).map(element -> element.getFileName() + ":" + element.getLineNumber()).toList());
                board.resign(board.getFen().isWhiteToMove());
            }
//            new TreeView(currentMove);
            boardRepository.update(board);
            template.convertAndSend("/board/" + board.getId(), "update");
        }
    }

}
