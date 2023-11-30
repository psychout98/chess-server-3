package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class King extends Piece {

    private static final int points = 100;
    private static final int[][] baseMoves = {{0, 1}, {0, -1}, {1, 1}, {1, 0}, {1, -1}, {-1, 1}, {-1, 0}, {-1, -1}};
    private static final int[][] castleMoves = {{0, 2}, {0, 6}, {7, 2}, {7, 6}};
    private static final int[][][] castleSpaces = {{{0, 1}, {0, 2}, {0, 3}}, {{0, 5}, {0, 6}}, {{7, 1}, {7, 2}, {7, 3}}, {{7, 5}, {7, 6}}};
    private static final int[][] checkSpaces = {{0, 3}, {0, 5}, {7, 3}, {7, 5}};

    public King(int row, int col, boolean white, boolean shallow, Board board) {
        super(row, col, white, new HashSet<>(), shallow, board);
    }

    @Override
    public void generateMoves() {
        addMoves(Set.of(baseMoves).stream().map(move -> new int[]{move[0] + getRow(), move[1] + getCol()})
                .filter(this::validateKingMove).collect(Collectors.toSet()));
        if (isWhite()) {
            if (getBoard().getCastle().get("0402") && validateCastle(0)) {
                addMove(castleMoves[0]);
            }
            if (getBoard().getCastle().get("0406") && validateCastle(1)) {
                addMove(castleMoves[1]);
            }
        } else {
            if (getBoard().getCastle().get("7472") && validateCastle(2)) {
                addMove(castleMoves[2]);
            }
            if (getBoard().getCastle().get("7476") && validateCastle(3)) {
                addMove(castleMoves[3]);
            }
        }
    }

    @Override
    public int getPoints() {
        return points;
    }

    private boolean validateKingMove(int[] move) {
        try {
            getBoard().validateKingMove(isWhite(), move);
            return true;
        } catch (InvalidMoveException e) {
            return false;
        }
    }

    private boolean emptyCastleSpaces(int castleNum) {
        for (int[] space : castleSpaces[castleNum]) {
            if (!getBoard().getBoardKey()[space[0]][space[1]].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean validateCastle(int castleNum) {
        return emptyCastleSpaces(castleNum) && validateKingMove(checkSpaces[castleNum]);
    }

}
