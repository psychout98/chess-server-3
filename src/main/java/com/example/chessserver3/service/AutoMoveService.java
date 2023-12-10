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
            board.move(currentMove.findBestFuture(depth).getMoveCode());
//            new TreeView(currentMove);
            boardRepository.update(board);
            template.convertAndSend("/board/" + board.getId(), "update");
        }
    }

}
