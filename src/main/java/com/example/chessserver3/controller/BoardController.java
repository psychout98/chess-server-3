package com.example.chessserver3.controller;

import com.example.chessserver3.exception.BoardNotFoundException;
import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.model.BoardResponse;
import com.example.chessserver3.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<BoardResponse> createBoard(HttpSession session, @RequestParam(required = false) String sessionId) {
        return new ResponseEntity<>(new BoardResponse(sessionId == null ? session.getId() : sessionId, boardService.createBoard(session.getId())), HttpStatus.OK);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(HttpSession session, @PathVariable String boardId,  @RequestParam(required = false) String sessionId) {
        return new ResponseEntity<>(new BoardResponse(sessionId == null ? session.getId() : sessionId, boardService.getBoard(boardId)), HttpStatus.OK);
    }

    @GetMapping("/{boardId}/{moveNumber}")
    public ResponseEntity<BoardResponse> getBoardAtMove(HttpSession session, @PathVariable String boardId, @PathVariable int moveNumber,  @RequestParam(required = false) String sessionId) {
        return new ResponseEntity<>(new BoardResponse(sessionId == null ? session.getId() : sessionId, boardService.getBoardAtMove(boardId, moveNumber)), HttpStatus.OK);
    }

    @PutMapping("/{boardId}/join")
    public ResponseEntity<String> join(HttpSession session, @PathVariable String boardId,  @RequestParam(required = false) String sessionId) {
        boardService.join(boardId, sessionId == null ? session.getId() : sessionId);
        return new ResponseEntity<>(session.getId(), HttpStatus.OK);
    }

    @PutMapping("/{boardId}/move/{moveCode}")
    public ResponseEntity<BoardResponse> move(HttpSession session, @PathVariable String boardId, @PathVariable String moveCode, @RequestParam(required = false) String sessionId) {
        return new ResponseEntity<>(new BoardResponse(sessionId == null ? session.getId() : sessionId, boardService.move(boardId, session.getId(), moveCode)), HttpStatus.OK);
    }
}
