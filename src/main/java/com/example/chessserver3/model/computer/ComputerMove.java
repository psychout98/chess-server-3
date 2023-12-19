package com.example.chessserver3.model.computer;

import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Castle;
import lombok.*;

import static com.example.chessserver3.model.board.Move.*;

@Getter
public class ComputerMove {

    private final String moveCode;
    private final char key;
    private final boolean white;
    private final byte startRow;
    private final byte endRow;
    private final byte startCol;
    private final byte endCol;
    private final char endKey;
    private boolean castleMove;
    private boolean enPassant;
    private boolean pushTwo;
    private boolean kingKiller;
    private final AnalysisBoard analysisBoard;

    public ComputerMove(final byte[] moveArray, final ShortFEN previousShortFEN, byte depth, byte maxDepth) {
        startRow = moveArray[0];
        startCol = moveArray[1];
        endRow = moveArray[2];
        endCol = moveArray[3];
        key = previousShortFEN.getBoardKey()[startRow][startCol];
        white = Character.isUpperCase(key);
        if (white != previousShortFEN.isWhiteToMove()) {
            throw new InvalidMoveException("Not your turn");
        }
        enPassant = false;
        castleMove = false;
        pushTwo = false;
        kingKiller = false;
        moveCode = String.format("%s%s%s%s", startRow, startCol, endRow, endCol);
        boolean pawnMove = key == 'p' || key == 'P';
        char[][] boardKey = Board.copyBoardKey(previousShortFEN.getBoardKey());
        endKey = boardKey[endRow][endCol];
        if (isObstructed(boardKey)) {
            throw new InvalidMoveException("Obstruction");
        }
        if (pawnMove) {
            runPawnMove(boardKey, previousShortFEN.getEnPassantTarget());
        } else if ((key == 'k' || key == 'K') && castle.get(moveCode) != null && previousShortFEN.getCastles().contains(Castle.castleKeys.get(moveCode))) {
            runCastle(boardKey);
        } else if ((key == 'k' || key == 'K') && Math.abs(endCol - startCol) == 2) {
            throw new InvalidMoveException("Invalid castle");
        } else {
            runBasicMove(boardKey);
        }
        if (white ? endKey == 'k' : endKey == 'K') {
            kingKiller = true;
        }
        analysisBoard = new AnalysisBoard(ShortFEN.updateFEN(previousShortFEN, boardKey, key, endKey, startCol, endCol, pushTwo ? enPassantTarget() : "-"), (byte) (depth + 1), maxDepth, moveCode);
    }

    private String enPassantTarget() {
        return Board.spaceToSpace(new byte[]{(byte) (white ? startRow - 1 : startRow + 1), startCol});
    }

    private boolean isObstructed(char[][] boardKey) {
        boolean obstructed = false;
        boolean open = endKey == 'x' || (Character.isLowerCase(key) != Character.isLowerCase(endKey));
        if (kingsAndKnights.contains(String.valueOf(key))) {
            obstructed = !open;
        }
        if (queensAndBishops.contains(String.valueOf(key))) {
            obstructed = diagonalObstruction(boardKey) || !open;
        }
        if (queensAndRooksAndPawns.contains(String.valueOf(key))) {
            obstructed = obstructed || (straightObstruction(boardKey) || !open);
        }
        return obstructed;
    }

    private boolean diagonalObstruction(char[][] boardKey) {
        int vertical = endRow - startRow;
        int horizontal = endCol - startCol;
        if (Math.abs(vertical) != Math.abs(horizontal)) {
            return false;
        } else if (vertical > 0 && horizontal > 0) {
            for (int i=1; i<vertical; i++) {
                if (boardKey[startRow + i][startCol + i] != 'x') {
                    return true;
                }
            }
        } else if (vertical > 0 && horizontal < 0) {
            for (int i=1; i<vertical; i++) {
                if (boardKey[startRow + i][startCol - i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0 && horizontal > 0) {
            for (int i=-1; i>vertical; i--) {
                if (boardKey[startRow + i][startCol - i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0 && horizontal < 0) {
            for (int i=-1; i>vertical; i--) {
                if (boardKey[startRow + i][startCol + i] != 'x') {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean straightObstruction(char[][] boardKey) {
        int vertical = endRow - startRow;
        int horizontal = endCol - startCol;
        if (vertical != 0 && horizontal != 0) {
            return false;
        } else if (vertical == 0 && horizontal < 0) {
            for (int i=endCol + 1; i<startCol; i++) {
                if (boardKey[startRow][i] != 'x') {
                    return true;
                }
            }
        } else if (vertical == 0 && horizontal > 0){
            for (int i=startCol + 1; i<endCol; i++) {
                if (boardKey[startRow][i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0) {
            for (int i=endRow + 1; i<startRow; i++) {
                if (boardKey[i][startCol] != 'x') {
                    return true;
                }
            }
        } else {
            for (int i=startRow + 1; i<endRow; i++) {
                if (boardKey[i][startCol] != 'x') {
                    return true;
                }
            }
        }
        return false;
    }

    private void runBasicMove(char[][] boardKey) {
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
    }

    private void runPawnMove(char[][] boardKey, byte[] enPassantTarget) {
        if (Math.abs(endRow - startRow) == 2) {
            pushTwo = true;
            if (endKey == 'x' && (white ? startRow == 6 : startRow == 1)) {
                runBasicMove(boardKey);
            } else {
                throw new InvalidMoveException("Invalid push two");
            }
        } else if (startCol != endCol) {
            if (isEnPassant(enPassantTarget)) {
                runEnPassant(boardKey);
            } else {
                if (endKey == 'x' || Character.isLowerCase(key) == Character.isLowerCase(endKey)) {
                    throw new InvalidMoveException("Invalid pawn attack");
                }
                if (endRow == (white ? 0 : 7)) {
                    runQueenPromotion(boardKey);
                } else {
                    runBasicMove(boardKey);
                }
            }
        } else {
            if (endKey != 'x') {
                throw new InvalidMoveException("Obstructed pawn push");
            }
            if (endRow == (white ? 0 : 7)) {
                runQueenPromotion(boardKey);
            } else {
                runBasicMove(boardKey);
            }
        }
    }

    private void runEnPassant(char[][] boardKey) {
        int targetRow = white ? endRow + 1 : endRow - 1;
        enPassant = true;
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[targetRow][endCol] = 'x';
    }

    private void runQueenPromotion(char[][] boardKey) {
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = white ? 'Q' : 'q';
    }

    private void runCastle(char[][] boardKey) {
        for (byte[] space : Castle.castleRoutes.get(moveCode)) {
            if (boardKey[space[0]][space[1]] != 'x') {
                throw new InvalidMoveException("Obstructed castle");
            }
        }
        castleMove = true;
        byte[] rookMove = Castle.castleRookMove.get(moveCode);
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[rookMove[0]][rookMove[1]] = 'x';
        boardKey[rookMove[2]][rookMove[3]] = white ? 'R' : 'r';
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
