package com.example.chessserver3.model.board;


import com.example.chessserver3.exception.InvalidMoveException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"myMove", "key", "white", "startRow", "endRow", "startCol", "endCol",
        "fenString", "position", "previousFen", "endKey", "castleMove", "enPassant", "pushTwo",
        "futures", "checkmate", "queensAndRooksAndPawns", "queensAndBishops", "kingsAndKnights",
        "castle", "gradient", "pointValues", "rt"})
public class Move {

    private String moveCode;
    private String moveString;
    private boolean valid;
    private boolean myMove;
    private char key;
    private boolean white;
    private byte startRow;
    private byte endRow;
    private byte startCol;
    private byte endCol;
    private String fenString;
    private String position;
    private FEN previousFen;
    private char endKey;
    private boolean castleMove;
    private boolean enPassant;
    private boolean pushTwo;
    private Set<Move> futures;
    public static final String queensAndRooksAndPawns = "qQrRpP";
    public static final String queensAndBishops = "qQbB";
    public static final String kingsAndKnights = "kKnN";
    public static final HashMap<String, String> castle = new HashMap<>();
    public static final byte[] gradient = {0, 1, 2, 4, 4, 2, 1, 0};
    public static final HashMap<Character, Integer> pointValues = new HashMap<>();
    static {
        castle.put("0402", "q");
        castle.put("0406", "k");
        castle.put("7472", "Q");
        castle.put("7476", "K");
        pointValues.put('q', -2521);
        pointValues.put('Q', 2521);
        pointValues.put('r', -1270);
        pointValues.put('R', 1270);
        pointValues.put('b', -836);
        pointValues.put('B', 836);
        pointValues.put('n', -817);
        pointValues.put('N', 817);
        pointValues.put('p', -198);
        pointValues.put('P', 198);
        pointValues.put('k', -300);
        pointValues.put('K', 300);
        pointValues.put('x', 0);
    }

    public Move(final byte[] moveArray, final FEN previousFen) {
        startRow = moveArray[0];
        startCol = moveArray[1];
        endRow = moveArray[2];
        endCol = moveArray[3];
        this.previousFen = previousFen;
        key = previousFen.getBoardKey()[startRow][startCol];
        white = Character.isUpperCase(key);
        myMove = white == previousFen.isWhiteToMove();
        enPassant = false;
        moveString = "";
        castleMove = false;
        pushTwo = false;
        moveCode = String.format("%s%s%s%s", startRow, startCol, endRow, endCol);
        boolean pawnMove = key == 'p' || key == 'P';
        char[][] boardKey = Board.copyBoardKey(previousFen.getBoardKey());
        endKey = boardKey[endRow][endCol];
        fenString = previousFen.getFen();
        boolean free = endKey == 'x';
        valid = !isObstructed(boardKey);
        moveString += pawnMove ? (startCol == endCol ? "" : (char) (startCol + 97)) : (white ? Character.toLowerCase(key) : key);
        if (pawnMove) {
            runPawnMove(boardKey, free, previousFen.getEnPassantTarget());
        } else if ((key == 'k' || key == 'K') && castle.get(moveCode) != null && previousFen.getCastles().contains(castle.get(moveCode))) {
            runCastle(boardKey);
        } else if ((key == 'k' || key == 'K') && Math.abs(endCol - startCol) == 2) {
            valid = false;
            runBasicMove(boardKey, free);
        } else {
            runBasicMove(boardKey, free);
        }
        fenString = FEN.updateFEN(previousFen, boardKey, key, endKey, startCol, endCol, pushTwo ? enPassantTarget() : "-");
        position = FEN.getBoardField(fenString);
        futures = new HashSet<>();
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

    private void runBasicMove(char[][] boardKey, boolean free) {
        if (!free) {
            moveString += "x";
        }
        moveString += (char) (endCol + 97);
        moveString += 8 - endRow;
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
    }

    private void runPawnMove(char[][] boardKey, boolean free, byte[] enPassantTarget) {
        if (Math.abs(endRow - startRow) == 2) {
            pushTwo = true;
            valid = valid && endKey == 'x' && (white ? startRow == 6 : startRow == 1);
            runBasicMove(boardKey, free);
        } else if (startCol != endCol) {
            if (isEnPassant(enPassantTarget)) {
                runEnPassant(boardKey);
            } else {
                valid = valid && endKey != 'x' && Character.isLowerCase(key) != Character.isLowerCase(endKey);
                if (endRow == (white ? 0 : 7)) {
                    runQueenPromotion(boardKey, free);
                } else {
                    runBasicMove(boardKey, free);
                }
            }
        } else {
            valid = valid && endKey == 'x';
            if (endRow == (white ? 0 : 7)) {
                runQueenPromotion(boardKey, free);
            } else {
                runBasicMove(boardKey, free);
            }
        }
    }

    private void runEnPassant(char[][] boardKey) {
        int targetRow = white ? endRow + 1 : endRow - 1;
        enPassant = true;
        moveString += "x";
        moveString += (char) (endCol + 97);
        moveString += (targetRow + 1);
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[targetRow][endCol] = 'x';
    }

    private void runQueenPromotion(char[][] boardKey, boolean free) {
        if (!free) {
            moveString += "x";
        }
        moveString += (char) (endCol + 97);
        moveString += 8 - endRow;
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = white ? 'Q' : 'q';
    }

    private void runCastle(char[][] boardKey) {
        for (byte[] space : Castle.castleRoutes.get(moveCode)) {
            if (boardKey[space[0]][space[1]] != 'x') {
                valid = false;
                break;
            }
        }
        castleMove = true;
        moveString = Castle.castleMoveString.get(moveCode);
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

    public void generateFutures() {
        Board copyBoard = Board.builder()
                .fen(previousFen)
                .history(new ArrayList<>())
                .shallow(true)
                .build();
        copyBoard.update();
        try {
            copyBoard.move(moveCode);
            if (castleMove) {
                valid = valid && copyBoard.getMoves().values().stream()
                        .filter(future -> future.valid && future.myMove)
                        .noneMatch(future -> Arrays.stream(Castle.castleSpaces.get(moveCode))
                                .anyMatch(dest -> future.endRow == dest[0] && future.endCol == dest[1]));
            } else {
                valid = valid && !copyBoard.checkCheck(white);
            }
        } catch (InvalidMoveException e) {
            valid = false;
        }
        futures.addAll(copyBoard.getMoves().values().stream().filter(Move::isValid).filter(Move::isMyMove).collect(Collectors.toSet()));
    }
}
