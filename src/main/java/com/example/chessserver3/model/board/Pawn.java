package com.example.chessserver3.model.board;

import java.util.HashSet;

public class Pawn extends Piece{

    private static final int points = 1;
    public Pawn(int row, int col, boolean white, Board board) {
        super(row, col, white, new HashSet<>(), board);
    }

    @Override
    public void generateMoves() {
        int direction = isWhite() ? 1 : -1;
        int[] pushOne = {getRow() + direction, getCol()};
        if ((isWhite() && getRow() < 7 || !isWhite() && getRow() > 0) && !isObstructed(pushOne)) {
            addMove(pushOne);
        }
        if (getRow() == (isWhite() ? 1 : 6) && !isObstructed(pushOne)) {
            int[] pushTwo = {getRow() + 2 * direction, getCol()};
            if (!isObstructed(pushTwo)) {
                addMove(pushTwo);
            }
        }
        int[] attackRight = {getRow() + direction, getCol() + 1};
        if (isOnBoard(attackRight) && !isObstructed(attackRight, isWhite())) {
            addMove(attackRight);
        }
        int[] attackLeft = {getRow() + direction, getCol() - 1};
        if (isOnBoard(attackLeft) && !isObstructed(attackLeft, isWhite())) {
            addMove(attackLeft);
        }
    }

    @Override
    public int getPoints() {
        return points;
    }
}
