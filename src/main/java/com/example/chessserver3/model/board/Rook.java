package com.example.chessserver3.model.board;

import java.util.HashSet;

public class Rook extends Piece {

    public Rook(int row, int col, boolean white, Board board) {
        super(row, col, white, new HashSet<>(), board);
    }

    @Override
    public void generateMoves() {
        addRookMoves();
    }
}
