package com.example.chessserver3.model.board;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FutureBoard extends Thread {

    private Board board;
    private boolean shallow;
    private Move move;

    public void run() {
        Board deepCopy = board.copy(shallow, board.getCurrentMove());
        deepCopy.move(move.getMoveCode(), board.isWhiteToMove());
        board = deepCopy;
    }
}
