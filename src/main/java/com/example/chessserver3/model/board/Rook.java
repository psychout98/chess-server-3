package com.example.chessserver3.model.board;

import java.util.HashSet;

public class Rook extends Piece {

    private static final int points = 5;

    public Rook(int row, int col, boolean white, boolean shallow, Board board) {
        super(row, col, white, new HashSet<>(), shallow, board, false);
    }

    @Override
    public void generateMoves() {
        addRookMoves();
    }

    @Override
    public int getPoints() {
        return points;
    }
}
