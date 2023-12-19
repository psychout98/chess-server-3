package com.example.chessserver3.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
@Getter
public class InvalidMoveException extends RuntimeException {

    private final MoveException moveException;
    public InvalidMoveException(MoveException moveException) {
        super(moveException.getMessage());
        this.moveException = moveException;
    }

    public InvalidMoveException(String message) {
        super(message);
        this.moveException = MoveException.INVALID_MOVE;
    }
}
