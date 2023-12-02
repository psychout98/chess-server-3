package com.example.chessserver3.model.board;


import java.util.HashSet;

public class Bishop extends Piece {

    private static final int points = 3;

    public Bishop(int row, int col, boolean white, Board board) { super(row, col, white, new HashSet<>(), board); }

    @Override
    public void generateMoves() {
        addBishopMoves();
    }

    @Override
    public int getPoints() {
        return points;
    }

}
