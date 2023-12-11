package com.example.chessserver3.model.board;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PGN {

    private String moveString;
    private String moveCode;
    private String fen;
}
