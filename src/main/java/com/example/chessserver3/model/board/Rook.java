package com.example.chessserver3.model.board;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Rook extends Piece {

    private static final int points = 5;

    public Rook(int row, int col, boolean white, boolean shallow) { super(row, col, white, shallow); }

    @Override
    public void generateMoves(Board board) {
        addRookMoves(board);
    }

    @Override
    public int getPoints() {
        return points;
    }
}
