package com.example.chessserver3.model.board;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Pawn extends Piece{

    private static final int points = 1;
    public Pawn(int row, int col, boolean white, boolean shallow) {
        super(row, col, white, shallow);
    }

    @Override
    public void generateMoves(Board board) {
        int direction = isWhite() ? 1 : -1;
        int[] pushOne = {getRow() + direction, getCol()};
        if (getRow() < 7 && board.getBoardKey()[pushOne[0]][pushOne[1]].isEmpty()) {
            addMove(board, pushOne, false);
        }
        if (getRow() == (isWhite() ? 1 : 6) && !isObstructed(pushOne, board.getBoardKey(), isWhite()) && !isObstructed(pushOne, board.getBoardKey(), !isWhite())) {
            int[] pushTwo = {getRow() + 2 * direction, getCol()};
            if (board.getBoardKey()[pushTwo[0]][pushTwo[1]].isEmpty()) {
                addMove(board, pushTwo, false);
            }
        }
        int[] attackRight = {getRow() + direction, getCol() + 1};
        if (isOnBoard(attackRight) && (board.getBoardKey()[attackRight[0]][attackRight[1]].startsWith(isWhite() ? "b" : "w") || board.isEnPassant(attackRight, direction))) {
            addMove(board, attackRight);
        }
        int[] attackLeft = {getRow() + direction, getCol() - 1};
        if (isOnBoard(attackLeft) && (board.getBoardKey()[attackLeft[0]][attackLeft[1]].startsWith(isWhite() ? "b" : "w") || board.isEnPassant(attackLeft, direction))) {
            addMove(board, attackLeft);
        }
    }

    @Override
    public int getPoints() {
        return points;
    }
}
