package com.example.chessserver3.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private String username;
    private String password;
    private String playerId;
    private List<String> games;

    public void addGame(String boardId) {
        games.add(boardId);
    }
}
