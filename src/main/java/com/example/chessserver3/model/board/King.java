package com.example.chessserver3.model.board;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class King extends Piece {

    private static final int points = 100;
    private static final int[][] baseMoves = {{0, 1}, {0, -1}, {1, 1}, {1, 0}, {1, -1}, {-1, 1}, {-1, 0}, {-1, -1}};
    private static final HashMap<String, int[][]> castleMoves = new HashMap<>();
    static {
        castleMoves.put("0402", new int[][]{{0, 2}, {0, 3}});
        castleMoves.put("0406", new int[][]{{0, 5}, {0, 6}});
        castleMoves.put("7472", new int[][]{{7, 2}, {7, 3}});
        castleMoves.put("7476", new int[][]{{7, 5}, {7, 6}});
    }

    public King(int row, int col, boolean white, Board board) {
        super(row, col, white, new HashSet<>(), board);
    }

    @Override
    public void generateMoves() {
        addMoves(Set.of(baseMoves).stream().map(move -> new int[]{move[0] + getRow(), move[1] + getCol()})
                .filter(this::isOnBoard)
                .filter(move -> !isObstructed(move, isWhite()))
                .collect(Collectors.toSet()));
        getBoard().getCastle().forEach((key, value) -> {
            if (value && key.startsWith(isWhite() ? "0" : "7") && Arrays.stream(castleMoves.get(key)).noneMatch(this::isObstructed)) {
                addMove(key);
            }
        });
    }

    @Override
    public int getPoints() {
        return points;
    }

}
