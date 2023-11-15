package com.example.chessserver3.service;

import com.example.chessserver3.exception.BoardNotFoundException;
import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.model.Board;
import com.example.chessserver3.model.Player;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Objects;


@Service
@EnableMongoRepositories
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;

    private final static String boardKeyString = "wr1,wn1,wb1,wq,wk,wb2,wn2,wr2,wp1,wp2,wp3,wp4,wp5,wp6,wp7,wp8,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,bp1,bp2,bp3,bp4,bp5,bp6,bp7,bp8,br1,bn1,bb1,bq,bk,bb2,bn2,br2";

    public Board createBoard(String sessionId) {
//        String[][] boardKey = {{"wr1", "wn1", "wb1", "wq", "wk", "wb2", "wn2", "wr2"},
//                {"wp1", "wp2", "wp3", "wp4", "wp5", "wp6", "wp7", "wp8"},
//                {"", "", "", "", "", "", "", ""},
//                {"", "", "", "", "", "", "", ""},
//                {"", "", "", "", "", "", "", ""},
//                {"", "", "", "", "", "", "", ""},
//                {"bp1", "bp2", "bp3", "bp4", "bp5", "bp6", "bp7", "bp8"},
//                {"br1", "bn1", "bb1", "bq", "bk", "bb2", "bn2", "br2"}
//        };
        HashMap<String, Boolean> castle = new HashMap<>();
        castle.put("0402", true);
        castle.put("0406", true);
        castle.put("7472", true);
        castle.put("7476", true);
        Board board = new Board(
                new Player("liam", sessionId),
                null,
                boardKeyString,
                0,
                null,
                false,
                false,
                false,
                castle);
        boardRepository.create(board);
        return board;
    }

    public void join(String boardId, String sessionId) {
        Board board = getBoard(boardId);
        if (board.getBlack() == null) {
            board.setBlack(new Player("jacob", sessionId));
        } else if (board.getWhite() == null) {
            board.setWhite(new Player("jacob", sessionId));
        } else {
            throw new InvalidMoveException("Cannot join full game");
        }
        boardRepository.update(board);
        boardRepository.update(board);
    }

    public Board getBoard(String boardId) {
        Board board = boardRepository.findById(boardId);
        if (board != null) {
            board.updateBoard();
            return board;
        } else {
            throw new BoardNotFoundException("Board id=" + boardId + " not found");
        }
    }

    public Board move(String boardId, String sessionId, String moveCode) {
        Board board = getBoard(boardId);
        if (Objects.equals(moveCode, "resign")) {
            if (Objects.equals(board.getWhite().getSessionId(), sessionId)) {
                board.resign(true);
            } else if (Objects.equals(board.getBlack().getSessionId(), sessionId)) {
                board.resign(false);
            } else {
                throw new InvalidMoveException("Invalid sessionId");
            }
        } else {
            board.move(moveCode);
        }
        boardRepository.update(board);
        return board;
    }

    public Board getBoardAtMove(String boardId, int moveNumber) {
        Board board = getBoard(boardId);
        if (board.getCurrentMove() != moveNumber) {
            board.setBoardKey(board.boardKeyStringToArray(board.getHistory().get(moveNumber).getBoardKeyString()));
            board.setShallow(true);
        }
        return board;
    }
}
