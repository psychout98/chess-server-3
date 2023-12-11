package com.example.chessserver3.service;

import com.example.chessserver3.exception.BoardNotFoundException;
import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.exception.UnsupportedDepthException;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.FEN;
import com.example.chessserver3.model.board.Move;
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
    private final static Move firstMove = new Move();
    static {
        firstMove.setFenString(initialFEN);
        firstMove.setMoveCode("");
        firstMove.setMoveString("-");
        firstMove.setKey('x');
    }
    private final static Random random = new Random();

    public Board createBoard(Player white, Player black) {
        Board board = Board.builder()
                .id(new ObjectId().toHexString())
                .white(white)
                .black(black)
                .fen(new FEN(initialFEN))
                .lastMove(firstMove)
                .history(new ArrayList<>())
                .shallow(false)
                .build();
        board.update();
        if (white != null && Objects.equals(white.getName(), "computer")) {
            List<Move> moves = board.getMoves().values().stream().filter(Move::isValid).filter(Move::isMyMove).toList();
            board.move(moves.get(random.nextInt(moves.size() - 1)).getMoveCode());
        }
        boardRepository.create(board);
        if (white != null) {
            userService.addGameToUser(white.getId(), board.getId());
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
            if (Objects.equals(board.getWhite().getId(), player.getId()) || Objects.equals(board.getBlack().getId(), player.getId())) {
                board.move(moveCode);
            }else {
                throw new InvalidMoveException("Invalid Id");
            }
        }
        boardRepository.update(board);
        if (Objects.equals(board.getWhite().getName(), "computer")) {
            computerMove(board, true);
        } else if (Objects.equals(board.getBlack().getName(), "computer")) {
            computerMove(board, false);
        }
        return board;
    }

    private void computerMove(Board board, boolean white) {
        String id = white ? board.getWhite().getId() : board.getBlack().getId();
        try {
            int depth = Integer.parseInt(id.split("-")[1]);
            if (depth < 10) {
                autoMoveService.autoMove(board, depth);
            } else {
                throw new UnsupportedDepthException("Level 10 and above not yet supported by system");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new UnsupportedDepthException("Unsupported or invalid depth provided");
        }
    }

    public Board getBoardAtMove(String boardId, int moveNumber) {
        Board board = getBoard(boardId);
        if (board.getHistory().size() - 1 != moveNumber) {
            Board copyBoard = Board.builder()
                    .fen(new FEN(board.getHistory().get(moveNumber).getFen()))
                    .lastMove(firstMove)
                    .history(board.getHistory())
                    .shallow(true)
                    .build();
            copyBoard.update();
            return copyBoard;
        } else {
            return board;
        }
    }

    public List<Board> getBoardsByPlayerName(String playerName) {
        return userService.getBoardIdsByPlayerName(playerName).stream().map(this::getBoard).collect(Collectors.toList());
    }
}
