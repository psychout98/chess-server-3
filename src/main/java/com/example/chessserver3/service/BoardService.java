package com.example.chessserver3.service;

import com.example.chessserver3.exception.BoardNotFoundException;
import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.FEN;
import com.example.chessserver3.model.board.Player;
import com.example.chessserver3.repository.BoardRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@EnableMongoRepositories
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AutoMoveService autoMoveService;
    private final static String initialFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public Board createBoard(Player white, Player black) {
        Board board = Board.builder()
                .id(new ObjectId().toHexString())
                .white(white)
                .black(black)
                .fen(new FEN(initialFEN))
                .lastMoveCode("")
                .history(new ArrayList<>())
                .shallow(false)
                .build();
        board.update();
        boardRepository.create(board);
        if (white != null) {
            if (Objects.equals(white.getName(), "computer")) {
                autoMoveService.autoMove(board, (byte) Integer.parseInt(white.getId().split("-")[1]));
            } else {
                userService.addGameToUser(white.getId(), board.getId());
            }
        }
        if (black != null) {
            userService.addGameToUser(black.getId(), board.getId());
        }
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
    }

    public Board getBoard(String boardId) {
        Board board = boardRepository.findById(boardId);
        if (board != null) {
            board.getFen().build();
            board.update();
            autoMove(board);
            return board;
        } else {
            throw new BoardNotFoundException("Board id=" + boardId + " not found");
        }
    }

    private void autoMove(Board board) {
        if (board.getWinner() == 0 && board.getWhite() != null && board.getBlack() != null) {
            if (Objects.equals(board.getWhite().getName(), "computer") && board.getFen().isWhiteToMove()) {
                autoMoveService.autoMove(board, (byte) Integer.parseInt(board.getWhite().getId().split("-")[1]));
            } else if (Objects.equals(board.getBlack().getName(), "computer") && !board.getFen().isWhiteToMove()) {
                autoMoveService.autoMove(board, (byte) Integer.parseInt(board.getBlack().getId().split("-")[1]));
            }
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
            if (Objects.equals(board.getWhite().getId(), player.getId()) || Objects.equals(board.getBlack().getId(), player.getId())) {
                board.move(moveCode);
            }else {
                throw new InvalidMoveException("Invalid Id");
            }
        }
        autoMove(board);
        boardRepository.update(board);
        return board;
    }

    public List<Board> getBoardsByPlayerName(String playerName) {
        return userService.getBoardIdsByPlayerName(playerName).stream().map(this::getBoard).collect(Collectors.toList());
    }
}
