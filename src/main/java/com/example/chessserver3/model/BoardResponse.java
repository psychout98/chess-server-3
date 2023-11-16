package com.example.chessserver3.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardResponse {

    private Player player;
    private Board board;
}
