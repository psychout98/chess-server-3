package com.example.chessserver3.exception;

import lombok.Getter;

@Getter
public enum MoveException {

    INVALID_MOVE("Move is invalid"),
    NOT_YOUR_TURN("Not your turn"),
    OBSTRUCTED_PATH("Obstruction"),
    INVALID_CASTLE("Invalid castle"),
    INVALID_PUSH_ONE("Obstructed pawn push"),
    INVALID_PUSH_TWO("Invalid push two"),
    INVALID_PAWN_ATTACK("Invalid pawn attack"),
    GAME_IS_OVER("Game is over");

    private final String message;
    MoveException(String message) {
        this.message = message;
    }
}
