package com.example.chessserver3.model.board;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Queen extends Piece {

    private static final int points = 9;

    public Queen(int row, int col, boolean white, boolean shallow) { super(row, col, white, shallow); }

    @Override
    public void generateMoves(Board board) {
        addRookMoves(board);
        addBishopMoves(board);
    }

    @Override
    public int getPoints() {
        return points;
    }
}
