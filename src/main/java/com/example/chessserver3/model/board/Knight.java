package com.example.chessserver3.model.board;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Knight extends Piece {
    private static final int[][] baseMoves = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};

    public Knight(int row, int col, boolean white, Board board) {
        super(row, col, white, new HashSet<>(), board);
    }

    @Override
    public void generateMoves() {
        addMoves(Set.of(baseMoves).stream().map(move -> new int[]{move[0] + getRow(), move[1] + getCol()})
                .filter(this::isOnBoard)
                .filter(move -> !isObstructed(move, isWhite()))
                .collect(Collectors.toSet()));
    }
}
