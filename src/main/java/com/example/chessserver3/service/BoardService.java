package com.example.chessserver3.service;

import com.example.chessserver3.exception.BoardNotFoundException;
import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Player;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Objects;


@Service
@EnableMongoRepositories
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private UserService userService;

    private final static String boardKeyString = "wr1,wn1,wb1,wq,wk,wb2,wn2,wr2,wp1,wp2,wp3,wp4,wp5,wp6,wp7,wp8,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,bp1,bp2,bp3,bp4,bp5,bp6,bp7,bp8,br1,bn1,bb1,bq,bk,bb2,bn2,br2";

    public Board createBoard(Player player) {
        HashMap<String, Boolean> castle = new HashMap<>();
        castle.put("0402", true);
        castle.put("0406", true);
        castle.put("7472", true);
        castle.put("7476", true);
        Board board = new Board(
                player,
                null,
                boardKeyString,
                0,
                null,
                false,
                false,
                false,
                castle);
        boardRepository.create(board);
        userService.addGameToUser(player.getId(), board.getId());
        return board;
    }

    public void join(String boardId, Player player) {
        userService.addGameToUser(player.getId(), boardId);
        Board board = getBoard(boardId);
        if (board.getBlack() == null) {
            board.setBlack(player);
        } else if (board.getWhite() == null) {
            board.setWhite(player);
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

    public Board move(String boardId, Player player, String moveCode) {
        Board board = getBoard(boardId);
        if (Objects.equals(moveCode, "resign")) {
            if (Objects.equals(board.getWhite().getId(), player.getId())) {
                board.resign(true);
            } else if (Objects.equals(board.getBlack().getId(), player.getId())) {
                board.resign(false);
            } else {
                throw new InvalidMoveException("Invalid Id");
            }
        } else {
            if (Objects.equals(board.getWhite().getId(), player.getId())) {
                board.move(moveCode, true);
            } else if (Objects.equals(board.getBlack().getId(), player.getId())) {
                board.move(moveCode, false);
            } else {
                throw new InvalidMoveException("Invalid Id");
            }
        }
        boardRepository.update(board);
        return board;
    }

    public Board getBoardAtMove(String boardId, int moveNumber) {
        Board board = getBoard(boardId);
        if (board.getCurrentMove() != moveNumber) {
            board.setBoardKey(board.boardKeyStringToArray(board.getHistory().get(moveNumber).getBoardKeyString()));
            board.setCurrentMove(moveNumber);
            board.setCheck(false);
            board.setShallow(true);
        }
        return board;
    }
}
