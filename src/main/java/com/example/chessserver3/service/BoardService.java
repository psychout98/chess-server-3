package com.example.chessserver3.service;

import com.example.chessserver3.exception.BoardNotFoundException;
import com.example.chessserver3.model.Board;
import com.example.chessserver3.model.Player;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@EnableMongoRepositories
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    public Board createBoard(String sessionId) {
        String[][] boardKey = {{"wr1", "wn1", "wb1", "wq", "wk", "wb2", "wn2", "wr2"},
                {"wp1", "wp2", "wp3", "wp4", "wp5", "wp6", "wp7", "wp8"},
                {"", "", "", "", "", "", "", ""},
                {"", "", "", "", "", "", "", ""},
                {"", "", "", "", "", "", "", ""},
                {"", "", "", "", "", "", "", ""},
                {"bp1", "bp2", "bp3", "bp4", "bp5", "bp6", "bp7", "bp8"},
                {"br1", "bn1", "bb1", "bq", "bk", "bb2", "bn2", "br2"}
        };
        return boardRepository.save(new Board(new Player("noah", sessionId), null, boardKey, 0, null, false, false, false));
    }

    public Board getBoard(String sessionId, String boardId) {
        Optional<Board> boardResponse = boardRepository.findById(boardId);
        if (boardResponse.isPresent()) {
            Board board = boardResponse.get();
            if (Objects.equals(sessionId, board.getWhite().getSessionId())) {
                return board;
            } else if (board.getBlack() == null) {
                board.setBlack(new Player("liam", sessionId));
                return boardRepository.save(board);
            } else if (Objects.equals(sessionId, board.getBlack().getSessionId())) {
                return board;
            } else {
                return board;
            }
        } else {
            throw new BoardNotFoundException("Board id=" + sessionId + " not found");
        }
    }

    public Board move(String sessionId, String boardId, String moveCode) {
        Board board = getBoard(sessionId, boardId);
        board.move(moveCode);
        simpMessagingTemplate.convertAndSend(String.format("wss://pacific-refuge-56148-96967b0a6dc5.herokuapp.com/board/%s", boardId), "move");
        return boardRepository.save(board);
    }
}
