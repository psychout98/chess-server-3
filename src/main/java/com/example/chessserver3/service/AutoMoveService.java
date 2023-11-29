package com.example.chessserver3.service;

import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Move;
import com.example.chessserver3.model.board.Piece;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.Random;
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
        Thread.sleep(1000);
        Set<Move> moves = board.getPieces().values().stream().filter(piece -> piece.isWhite() == board.isWhiteToMove()).map(Piece::getMoves).flatMap(Set::stream).collect(Collectors.toSet());
        int i = new Random().nextInt(moves.size());
        Move move = moves.stream().toList().get(i);
        board.move(move.getMoveCode(), white);
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
        Thread.currentThread().interrupt();
    }
}
