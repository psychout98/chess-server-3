package com.example.chessserver3.model.board;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Bishop extends Piece {

    private static final int points = 3;

    public Bishop(int row, int col, boolean white, boolean shallow) { super(row, col, white, shallow); }

    @Override
    public void generateMoves(Board board) {
        addBishopMoves(board);
    }

    @Override
    public int getPoints() {
        return points;
    }

}
