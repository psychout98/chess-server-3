package com.example.chessserver3.controller;

import com.example.chessserver3.model.user.UserResponse;
import com.example.chessserver3.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin(origins = {"https://psychout98.github.io", "http://localhost:3000"}, allowCredentials = "true")
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public ResponseEntity<UserResponse> getUser(@RequestHeader(value = "username") String username, @RequestHeader(value = "password") String password) {
        return new ResponseEntity<>(new UserResponse(username, userService.getPlayerIdForUser(username, password)), HttpStatus.OK);
    }

    @PostMapping("/")
    public ResponseEntity<UserResponse> createUser(HttpSession session, @RequestHeader(value = "username") String username, @RequestHeader(value = "password") String password) {
        userService.createUser(username, password, session.getId());
        return new ResponseEntity<>(new UserResponse(username, session.getId()), HttpStatus.OK);
    }
}
