package com.example.chessserver3.model.board;

import java.util.HashSet;

public class Queen extends Piece {

    public Queen(int row, int col, boolean white, Board board) {
        super(row, col, white, new HashSet<>(), board);
    }

    @Override
    public void generateMoves() {
        addRookMoves();
        addBishopMoves();
    }
}
