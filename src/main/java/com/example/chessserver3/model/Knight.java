package com.example.chessserver3.model;

import java.util.Set;
import java.util.stream.Collectors;

public class Knight extends Piece {

    private static final int[][] baseMoves = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};

    public Knight(int row, int col, boolean white, boolean shallow) { super(row, col, white, shallow); }

    @Override
    public void generateMoves(Board board) {
        addMoves(Set.of(baseMoves).stream().map(move -> new int[]{move[0] + getRow(), move[1] + getCol()})
                .collect(Collectors.toSet()), board);
    }
}
