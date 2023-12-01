package com.example.chessserver3.model.board;

import java.util.HashSet;

public class Pawn extends Piece{

    private static final int points = 1;
    public Pawn(int row, int col, boolean white, boolean shallow, Board board) {
        super(row, col, white, new HashSet<>(), shallow, board, false);
    }

    @Override
    public void generateMoves() {
        int direction = isWhite() ? 1 : -1;
        int[] pushOne = {getRow() + direction, getCol()};
        if ((isWhite() && getRow() < 7 || !isWhite() && getRow() > 0) && getBoard().getBoardKey()[pushOne[0]][pushOne[1]].isEmpty()) {
            addMove(pushOne, false);
        }
        if (getRow() == (isWhite() ? 1 : 6) && !isObstructed(pushOne, isWhite()) && !isObstructed(pushOne, !isWhite())) {
            int[] pushTwo = {getRow() + 2 * direction, getCol()};
            if (getBoard().getBoardKey()[pushTwo[0]][pushTwo[1]].isEmpty()) {
                addMove(pushTwo, false);
            }
        }
        int[] attackRight = {getRow() + direction, getCol() + 1};
        if (isOnBoard(attackRight) && (getBoard().getBoardKey()[attackRight[0]][attackRight[1]].startsWith(isWhite() ? "b" : "w") || getBoard().isEnPassant(attackRight, direction))) {
            addMove(attackRight);
        }
        int[] attackLeft = {getRow() + direction, getCol() - 1};
        if (isOnBoard(attackLeft) && (getBoard().getBoardKey()[attackLeft[0]][attackLeft[1]].startsWith(isWhite() ? "b" : "w") || getBoard().isEnPassant(attackLeft, direction))) {
            addMove(attackLeft);
        }
    }

    @Override
    public int getPoints() {
        return points;
    }
}
