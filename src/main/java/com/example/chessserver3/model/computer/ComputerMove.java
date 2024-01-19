package com.example.chessserver3.model.computer;

import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.exception.MoveException;
import com.example.chessserver3.model.board.Castle;
import lombok.*;
import java.util.Objects;

import static com.example.chessserver3.model.board.Move.*;

@Getter
public class ComputerMove {

    private final String moveCode;
    private final char key;
    private String boardKey;
    private final boolean white;
    private final byte startRow;
    private final byte endRow;
    private final byte startCol;
    private final byte endCol;
    private final char endKey;
    private boolean enPassant;
    private boolean pushTwo;
    private final AnalysisBoard analysisBoard;

    public ComputerMove(final byte[] moveArray, final BoardData previousBoardData, byte depth, byte maxDepth) {
        boardKey = previousBoardData.getBoardKey();
        startRow = moveArray[0];
        startCol = moveArray[1];
        endRow = moveArray[2];
        endCol = moveArray[3];
        key = keyAtSpace(startRow, startCol);
        white = Character.isUpperCase(key);
        if (white != previousBoardData.isWhiteToMove()) {
            throw new InvalidMoveException(MoveException.NOT_YOUR_TURN);
        }
        enPassant = false;
        pushTwo = false;
        moveCode = String.format("%s%s%s%s", startRow, startCol, endRow, endCol);
        boolean pawnMove = key == 'p' || key == 'P';
        endKey = keyAtSpace(endRow, endCol);
        int newAdvantage = previousBoardData.getMaterialAdvantage() - Objects.requireNonNullElse(pointValues.get(endKey), 0);
        if (endKey != 'x' && Character.isLowerCase(key) == Character.isLowerCase(endKey)) {
            throw new InvalidMoveException(MoveException.OBSTRUCTED_PATH);
        }
        if (pawnMove) {
            runPawnMove(previousBoardData.getEnPassantTarget());
        } else if ((key == 'k' || key == 'K') && castle.get(moveCode) != null && previousBoardData.getCastles().contains(Castle.castleKeys.get(moveCode))) {
            runCastle();
        } else if ((key == 'k' || key == 'K') && Math.abs(endCol - startCol) == 2) {
            throw new InvalidMoveException(MoveException.INVALID_CASTLE);
        } else {
            runBasicMove();
        }
        analysisBoard = new AnalysisBoard(
                BoardData.updatedBoard(
                        previousBoardData,
                        boardKey,
                        key,
                        endKey,
                        startCol,
                        endCol,
                        pushTwo ? enPassantTarget() : null, newAdvantage
                ),
                (byte) (depth + 1),
                maxDepth,
                moveCode);
    }

    private void updateBoardKey(byte row, byte col, char key) {
        boardKey = boardKey.substring(0, (8 * row) + col) + key + boardKey.substring((8 * row) + col + 1);
    }

    private char keyAtSpace(byte row, byte col) {
        return boardKey.charAt((8 * row) + col);
    }

    private byte[] enPassantTarget() {
        return new byte[]{(byte) (white ? startRow - 1 : startRow + 1), startCol};
    }

    private void runBasicMove() {
        updateBoardKey(startRow, startCol, 'x');
        updateBoardKey(endRow, endCol, key);
    }

    private void runPawnMove(byte[] enPassantTarget) {
        if (Math.abs(endRow - startRow) == 2) {
            pushTwo = true;
            if (endKey == 'x' && (white ? startRow == 6 : startRow == 1) && keyAtSpace((byte) (white ? 5 : 2), startCol) == 'x') {
                runBasicMove();
            } else {
                throw new InvalidMoveException(MoveException.INVALID_PUSH_TWO);
            }
        } else if (startCol != endCol) {
            if (isEnPassant(enPassantTarget)) {
                runEnPassant();
            } else {
                if (endKey == 'x' || Character.isLowerCase(key) == Character.isLowerCase(endKey)) {
                    throw new InvalidMoveException(MoveException.INVALID_PAWN_ATTACK);
                }
                if (endRow == (white ? 0 : 7)) {
                    runQueenPromotion();
                } else {
                    runBasicMove();
                }
            }
        } else {
            if (endKey != 'x') {
                throw new InvalidMoveException(MoveException.INVALID_PUSH_ONE);
            }
            if (endRow == (white ? 0 : 7)) {
                runQueenPromotion();
            } else {
                runBasicMove();
            }
        }
    }

    private void runEnPassant() {
        byte targetRow = (byte) (white ? endRow + 1 : endRow - 1);
        enPassant = true;
        updateBoardKey(startRow, startCol, 'x');
        updateBoardKey(endRow, endCol, key);
        updateBoardKey(targetRow, endCol, 'x');
    }

    private void runQueenPromotion() {
        updateBoardKey(startRow, startCol, 'x');
        updateBoardKey(endRow, endCol, white ? 'Q' : 'q');
    }

    private void runCastle() {
        for (byte[] space : Castle.castleRoutes.get(moveCode)) {
            if (keyAtSpace(space[0], space[1]) != 'x') {
                throw new InvalidMoveException(MoveException.INVALID_CASTLE);
            }
        }
        byte[] rookMove = Castle.castleRookMove.get(moveCode);
        updateBoardKey(startRow, startCol, 'x');
        updateBoardKey(endRow, endCol, key);
        updateBoardKey(rookMove[0], rookMove[1], 'x');
        updateBoardKey(rookMove[2], rookMove[3], white ? 'R' : 'r');
    }

    public boolean isEnPassant(byte[] enPassantTarget) {
        if (enPassantTarget == null) {
            return false;
        } else {
            boolean attack = startCol != endCol;
            return attack && endRow == enPassantTarget[0] && endCol == enPassantTarget[1];
        }
    }
}
