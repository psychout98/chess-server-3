package com.example.chessserver3.model.board;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class King extends Piece {
    private static final int[][] baseMoves = {{0, 1}, {0, -1}, {1, 1}, {1, 0}, {1, -1}, {-1, 1}, {-1, 0}, {-1, -1}};

    public King(int row, int col, boolean white, Board board) {
        super(row, col, white, new HashSet<>(), board);
    }

    @Override
    public void generateMoves() {
        addMoves(Set.of(baseMoves).stream().map(move -> new int[]{move[0] + getRow(), move[1] + getCol()})
                .filter(this::isOnBoard)
                .filter(move -> !isObstructed(move, isWhite()))
                .collect(Collectors.toSet()));
        getBoard().getCastle().getValidCastles().forEach((key, value) -> {
            if (value && key.startsWith(isWhite() ? "0" : "7") && Arrays.stream(Castle.castleMoves.get(key)).noneMatch(this::isObstructed)) {
                addMove(key);
            }
        });
    }

}
