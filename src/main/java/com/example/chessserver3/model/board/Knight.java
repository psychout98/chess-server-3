package com.example.chessserver3.model.board;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Knight extends Piece {

    private static final int points = 3;
    private static final int[][] baseMoves = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};

    public Knight(int row, int col, boolean white, boolean shallow, Board board) {
        super(row, col, white, new HashSet<>(), shallow, board);
    }

    @Override
    public void generateMoves() {
        addMoves(Set.of(baseMoves).stream().map(move -> new int[]{move[0] + getRow(), move[1] + getCol()})
                .collect(Collectors.toSet()));
    }

    @Override
    public int getPoints() {
        return points;
    }
}
