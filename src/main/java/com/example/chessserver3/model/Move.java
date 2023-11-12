package com.example.chessserver3.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Move {

    private String moveCode;
    private String moveString;
    private String boardKeyString;
}
