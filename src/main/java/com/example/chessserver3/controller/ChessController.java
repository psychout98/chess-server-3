package com.example.chessserver3.controller;

import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.BoardResponse;
import com.example.chessserver3.model.board.Player;
import com.example.chessserver3.model.user.UserResponse;
import com.example.chessserver3.service.BoardService;
import com.example.chessserver3.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@CrossOrigin(origins = {"https://psychout98.github.io", "http://localhost:3000"}, allowCredentials = "true")
@RequestMapping("/")
public class ChessController {

    @Autowired
    BoardService boardService;
    @Autowired
    private UserService userService;

    @GetMapping("/user")
    public ResponseEntity<UserResponse> getUser(@RequestHeader(value = "username") String username, @RequestHeader(value = "password") String password) {
        return new ResponseEntity<>(new UserResponse(username.toLowerCase(), userService.getPlayerIdForUser(username.toLowerCase(), password)), HttpStatus.OK);
    }

    @PostMapping("/user")
    public ResponseEntity<UserResponse> createUser(HttpSession session, @RequestHeader(value = "username") String username, @RequestHeader(value = "password") String password) {
        userService.createUser(username.toLowerCase(), password, session.getId());
        return new ResponseEntity<>(new UserResponse(username.toLowerCase(), session.getId()), HttpStatus.OK);
    }

    @GetMapping("/player/{playerName}/boards")
    public ResponseEntity<List<Board>> getBoardsByPlayerName(@PathVariable String playerName) {
        return new ResponseEntity<>(boardService.getBoardsByPlayerName(playerName.toLowerCase()), HttpStatus.OK);
    }

    @PostMapping("/board")
    public ResponseEntity<BoardResponse> createBoard(HttpSession session, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName.toLowerCase(), playerId == null ? session.getId() : playerId);
        return new ResponseEntity<>(new BoardResponse(player, boardService.createBoard(player)), HttpStatus.OK);
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(HttpSession session, @PathVariable String boardId, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName.toLowerCase(), playerId == null ? session.getId() : playerId);
        return new ResponseEntity<>(new BoardResponse(player, boardService.getBoard(boardId)), HttpStatus.OK);
    }

    @GetMapping("/board/{boardId}/{moveNumber}")
    public ResponseEntity<BoardResponse> getBoardAtMove(HttpSession session, @PathVariable String boardId, @PathVariable int moveNumber, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName.toLowerCase(), playerId == null ? session.getId() : playerId);
        return new ResponseEntity<>(new BoardResponse(player, boardService.getBoardAtMove(boardId, moveNumber)), HttpStatus.OK);
    }

    @PutMapping("/board/{boardId}/join")
    public ResponseEntity<Player> join(HttpSession session, @PathVariable String boardId, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName.toLowerCase(), playerId == null ? session.getId() : playerId);
        boardService.join(boardId, player);
        return new ResponseEntity<>(player, HttpStatus.OK);
    }

    @PutMapping("/board/{boardId}/move/{moveCode}")
    public ResponseEntity<BoardResponse> move(HttpSession session, @PathVariable String boardId, @PathVariable String moveCode, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName.toLowerCase(), playerId == null ? session.getId() : playerId);
        return new ResponseEntity<>(new BoardResponse(player, boardService.move(boardId, player, moveCode)), HttpStatus.OK);
    }
}
