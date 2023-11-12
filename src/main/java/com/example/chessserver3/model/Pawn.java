package com.example.chessserver3.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Pawn extends Piece{

    public Pawn(int row, int col, boolean white, boolean shallow) {
        super(row, col, white, shallow);
    }

    @Override
    public void generateMoves(Board board) {
        int direction = isWhite() ? 1 : -1;
        int[] pushOne = {getRow() + direction, getCol()};
        if (board.getBoardKey()[pushOne[0]][pushOne[1]].isEmpty()) {
            addMove(pushOne, board);
        }
        if (getRow() == (isWhite() ? 1 : 6)) {
            int[] pushTwo = {getRow() + 2 * direction, getCol()};
            if (board.getBoardKey()[pushTwo[0]][pushTwo[1]].isEmpty()) {
                addMove(pushTwo, board);
            }
        }
        int[] attackRight = {getRow() + direction, getCol() + 1};
        if (isOnBoard(attackRight) && (board.getBoardKey()[attackRight[0]][attackRight[1]].startsWith(isWhite() ? "b" : "w") || board.enPassantable(attackRight, direction))) {
            addMove(attackRight, board);
        }
        int[] attackLeft = {getRow() + direction, getCol() - 1};
        if (isOnBoard(attackLeft) && (board.getBoardKey()[attackLeft[0]][attackLeft[1]].startsWith(isWhite() ? "b" : "w") || board.enPassantable(attackLeft, direction))) {
            addMove(attackLeft, board);
        }
    }

}
