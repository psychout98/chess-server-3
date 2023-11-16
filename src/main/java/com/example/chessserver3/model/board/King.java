package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
public class King extends Piece {

    private static final int[][] baseMoves = {{0, 1}, {0, -1}, {1, 1}, {1, 0}, {1, -1}, {-1, 1}, {-1, 0}, {-1, -1}};
    private static final int[][] castleMoves = {{0, 2}, {0, 6}, {7, 2}, {7, 6}};
    private static final int[][][] castleSpaces = {{{0, 1}, {0, 2}, {0, 3}}, {{0, 5}, {0, 6}}, {{7, 1}, {7, 2}, {7, 3}}, {{7, 5}, {7, 6}}};
    private static final int[][] checkSpaces = {{0, 3}, {0, 5}, {7, 3}, {7, 5}};

    public King(int row, int col, boolean white, boolean shallow) { super(row, col, white, shallow); }

    @Override
    public void generateMoves(Board board) {
        addMoves(board, Set.of(baseMoves).stream().map(move -> new int[]{move[0] + getRow(), move[1] + getCol()})
                .filter(move -> validateKingMove(board, move)).collect(Collectors.toSet()));
        if (isWhite()) {
            if (board.getCastle().get("0402") && validateCastle(board, 0)) {
                addMove(board, castleMoves[0]);
            }
            if (board.getCastle().get("0406") && validateCastle(board, 1)) {
                addMove(board, castleMoves[1]);
            }
        } else {
            if (board.getCastle().get("7472") && validateCastle(board, 2)) {
                addMove(board, castleMoves[2]);
            }
            if (board.getCastle().get("7476") && validateCastle(board, 3)) {
                addMove(board, castleMoves[3]);
            }
        }
    }

    private boolean validateKingMove(Board board, int[] move) {
        try {
            board.validateKingMove(isWhite(), move);
            return true;
        } catch (InvalidMoveException e) {
            return false;
        }
    }

    private boolean emptyCastleSpaces(String[][] boardKey, int castleNum) {
        boolean empty = true;
        for (int[] space : castleSpaces[castleNum]) {
            if (!boardKey[space[0]][space[1]].isEmpty()) {
                empty = false;
            }
        }
        return empty;
    }

    private boolean validateCastle(Board board, int castleNum) {
        return emptyCastleSpaces(board.getBoardKey(), castleNum) && validateKingMove(board, checkSpaces[castleNum]);
    }

}
