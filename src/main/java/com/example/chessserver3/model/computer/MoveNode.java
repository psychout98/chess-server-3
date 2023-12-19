package com.example.chessserver3.model.computer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MoveNode {

    private final byte[] moveArray;
    private final MoveNode next;

    public MoveNode(byte[] moveArray, byte dx, byte dy) {
        this.moveArray = moveArray;
        byte nextRow = (byte) (moveArray[2] + dx);
        byte nextCol = (byte) (moveArray[3] + dy);
        if (nextRow >=0 && nextRow < 8 && nextCol >=0 && nextCol < 8 && !(nextRow == moveArray[0] && nextCol == moveArray[1])) {
            byte[] nextMoveArray = {moveArray[0], moveArray[1], nextRow, nextCol};
            next = new MoveNode(nextMoveArray, dx, dy);
        } else {
            next = null;
        }
    }
}
