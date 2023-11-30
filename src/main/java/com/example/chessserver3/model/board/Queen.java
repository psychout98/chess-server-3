package com.example.chessserver3.model.board;

import java.util.HashSet;

public class Queen extends Piece {

    private static final int points = 9;

    public Queen(int row, int col, boolean white, boolean shallow, Board board) {
        super(row, col, white, new HashSet<>(), shallow, board);
    }

    @Override
    public void generateMoves() {
        addRookMoves();
        addBishopMoves();
    }

    @Override
    public int getPoints() {
        return points;
    }
}
