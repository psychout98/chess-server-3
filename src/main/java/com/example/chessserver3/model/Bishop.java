package com.example.chessserver3.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Bishop extends Piece {

    public Bishop(int row, int col, boolean white, boolean shallow) { super(row, col, white, shallow); }

    @Override
    public void generateMoves(Board board) {
        addBishopMoves(board);
    }

}
