package com.example.chessserver3.controller;

import com.example.chessserver3.model.BoardResponse;
import com.example.chessserver3.model.Player;
import com.example.chessserver3.service.BoardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin(origins = {"https://psychout98.github.io", "http://localhost:3000"}, allowCredentials = "true")
@RequestMapping("/board")
public class BoardController {

    @Autowired
    BoardService boardService;

    @PostMapping("")
    public ResponseEntity<BoardResponse> createBoard(HttpSession session, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName, playerId == null ? session.getId() : playerId);
        return new ResponseEntity<>(new BoardResponse(player, boardService.createBoard(player)), HttpStatus.OK);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(HttpSession session, @PathVariable String boardId, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName, playerId == null ? session.getId() : playerId);
        return new ResponseEntity<>(new BoardResponse(player, boardService.getBoard(boardId)), HttpStatus.OK);
    }

    @GetMapping("/{boardId}/{moveNumber}")
    public ResponseEntity<BoardResponse> getBoardAtMove(HttpSession session, @PathVariable String boardId, @PathVariable int moveNumber, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName, playerId == null ? session.getId() : playerId);
        return new ResponseEntity<>(new BoardResponse(player, boardService.getBoardAtMove(boardId, moveNumber)), HttpStatus.OK);
    }

    @PutMapping("/{boardId}/join")
    public ResponseEntity<Player> join(HttpSession session, @PathVariable String boardId, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName, playerId == null ? session.getId() : playerId);
        boardService.join(boardId, player);
        return new ResponseEntity<>(player, HttpStatus.OK);
    }

    @PutMapping("/{boardId}/move/{moveCode}")
    public ResponseEntity<BoardResponse> move(HttpSession session, @PathVariable String boardId, @PathVariable String moveCode, @RequestHeader(value = "playerId", required = false) String playerId, @RequestHeader(value = "playerName", required = false) String playerName) {
        Player player = new Player(playerName == null ? "anonymous" : playerName, playerId == null ? session.getId() : playerId);
        return new ResponseEntity<>(new BoardResponse(player, boardService.move(boardId, player, moveCode)), HttpStatus.OK);
    }
}
