package com.example.chessserver3.controller;

import com.example.chessserver3.model.BoardResponse;
import com.example.chessserver3.service.BoardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequestMapping("/board")
public class BoardController {

    @Autowired
    BoardService boardService;

    @PostMapping("")
    public ResponseEntity<BoardResponse> createBoard(HttpSession session) {
        return new ResponseEntity<>(new BoardResponse(session.getId(), boardService.createBoard(session.getId())), HttpStatus.OK);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(HttpSession session, @PathVariable String boardId) {
        return new ResponseEntity<>(new BoardResponse(session.getId(), boardService.getBoard(session.getId(), boardId)), HttpStatus.OK);
    }

    @PutMapping("/{boardId}/move/{moveCode}")
    public ResponseEntity<BoardResponse> move(HttpSession session, @PathVariable String boardId, @PathVariable String moveCode) {
        return new ResponseEntity<>(new BoardResponse(session.getId(), boardService.move(session.getId(), boardId, moveCode)), HttpStatus.OK);
    }
}
