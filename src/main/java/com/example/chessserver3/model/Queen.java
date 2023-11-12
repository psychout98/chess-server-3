package com.example.chessserver3.model;

public class Queen extends Piece {

    public Queen(int row, int col, boolean white, boolean shallow) { super(row, col, white, shallow); }

    @Override
    public void generateMoves(Board board) {
        addRookMoves(board);
        addBishopMoves(board);
    }
}
