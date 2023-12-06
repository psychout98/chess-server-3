package com.example.chessserver3.service;

import com.example.chessserver3.exception.BoardNotFoundException;
import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.exception.UnsupportedDepthException;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Castle;
import com.example.chessserver3.model.board.Move;
import com.example.chessserver3.model.board.Player;
import com.example.chessserver3.repository.BoardRepository;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private final static String initialBoardKeyString = "wr1,wn1,wb1,wq,wk,wb2,wn2,wr2,wp1,wp2,wp3,wp4,wp5,wp6,wp7,wp8,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,bp1,bp2,bp3,bp4,bp5,bp6,bp7,bp8,br1,bn1,bb1,bq,bk,bb2,bn2,br2";
    private final static Move firstMove = new Move();
    static {
        firstMove.setBoardKeyString(initialBoardKeyString);
        firstMove.setMoveCode("");
        firstMove.setMoveString("");
    }
    private static Random random = new Random();

    public Board createBoard(Player white, Player black) {
        Board board = Board.builder()
                .id(new ObjectId().toHexString())
                .white(white)
                .black(black)
                .boardKeyString(initialBoardKeyString)
                .pieces(new HashMap<>())
                .moves(new HashMap<>())
                .history(new ArrayList<>(List.of(firstMove)))
                .castle(Castle.initialCastle())
                .shallow(false)
                .whiteToMove(true)
                .build();
        board.update();
        if (white != null && Objects.equals(white.getName(), "computer")) {
            List<Move> moves = board.getMoves().values().stream().filter(Move::isValid).toList();
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
        if (!board.isCheckmate()) {
            if (Objects.equals(board.getWhite().getName(), "computer")) {
                computerMove(board, true);
            } else if (Objects.equals(board.getBlack().getName(), "computer")) {
                computerMove(board, false);
            }
        }
        return board;
    }

    private void computerMove(Board board, boolean white) {
        String id = white ? board.getWhite().getId() : board.getBlack().getId();
        try {
            int depth = Integer.parseInt(id.split("-")[1]);
            if (depth < 50) {
                autoMoveService.autoMove(board, depth);
            } else {
                throw new UnsupportedDepthException("Depth greater than 3 not yet supported by system");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new UnsupportedDepthException("Unsupported or invalid depth provided");
        }
    }

    public Board getBoardAtMove(String boardId, int moveNumber) {
        Board board = getBoard(boardId);
        if (board.getHistory().size() - 1 != moveNumber) {
            board.setBoardKey(Board.boardKeyStringToArray(board.getHistory().get(moveNumber).getBoardKeyString()));
            board.setMoves(Collections.emptyMap());
            board.setCheck(false);
        }
        return board;
    }

    public List<Board> getBoardsByPlayerName(String playerName) {
        return userService.getBoardIdsByPlayerName(playerName).stream().map(this::getBoard).collect(Collectors.toList());
    }
}
