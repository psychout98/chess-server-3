package com.example.chessserver3.model.computer;

import com.example.chessserver3.exception.InvalidFENException;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.FEN;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

import static com.example.chessserver3.model.board.Move.pointValues;

@Getter
@AllArgsConstructor
public class BoardData {

    private final char[][] boardKey;
    private final boolean whiteToMove;
    private final String castles;
    private final byte[] enPassantTarget;
    private int materialAdvantage;

    public BoardData(String fen) {
        materialAdvantage = 0;
        String[] fields = fen.split(" ");
        boardKey = new char[8][8];
        loadBoardKey(fields[0]);
        if (Objects.equals(fields[1], "w")) {
            whiteToMove = true;
        } else if (Objects.equals(fields[1], "b")) {
            whiteToMove = false;
        } else {
            throw new InvalidFENException("Incorrect FEN format : \"" + fen + "\"");
        }
        if (Arrays.stream(fields[2].split("")).anyMatch(key -> !FEN.validCastles.contains(key))) {
            throw new InvalidFENException("Invalid castle field: " + fields[2]);
        } else {
            castles = fields[2];
        }
        enPassantTarget = Objects.equals(fields[3], "-") ? null : Board.spaceToSpace(fields[3]);
    }

    public void loadBoardKey(String boardField) {
        String[] splitBoard = boardField.split("/");
        if (splitBoard.length == 8) {
            for (byte i = 0; i < 8; i++) {
                byte j = 0;
                for (char c : splitBoard[i].toCharArray()) {
                    if (FEN.validChars.contains(String.valueOf(c))) {
                        boardKey[i][j] = c;
                        materialAdvantage += calculatePoints(c);
                        j++;
                    } else if (c > 47 && c < 58) {
                        for (byte k = j; k < (j + c - 48); k++) {
                            boardKey[i][k] = 'x';
                        }
                        j += (byte) (c - 48);
                    } else {
                        throw new InvalidFENException("Invalid character \"" + c + "\" in FEN");
                    }
                }
            }
        } else {
            throw new InvalidFENException("Invalid position \"" + boardField + "\" in FEN");
        }
    }

    public static int calculatePoints(char key) {
        return Objects.requireNonNullElse(pointValues.get(key), 0);
    }

    public static BoardData updatedBoard(BoardData previousBoardData, char[][] boardKey, char key, char endKey, byte startCol, byte endCol, byte[] enPassantTarget, int newAdvantage) {
        return new BoardData(boardKey, !previousBoardData.whiteToMove, updateCastle(previousBoardData.castles, key, endKey, startCol, endCol), enPassantTarget, newAdvantage);
    }

    public static String updateCastle(String oldCastle, char key, char endKey, byte startCol, byte endCol) {
        HashMap<Character, Boolean> castles = new HashMap<>();
        castles.put('K', oldCastle.contains("K"));
        castles.put('Q', oldCastle.contains("Q"));
        castles.put('k', oldCastle.contains("k"));
        castles.put('q', oldCastle.contains("q"));
        if (key == 'K') {
            castles.put('K', false);
            castles.put('Q', false);
        } else if (key == 'k') {
            castles.put('k', false);
            castles.put('q', false);
        } else if (key == 'R') {
            if (startCol == 7) {
                castles.put('K', false);
            } else if (startCol == 0) {
                castles.put('Q', false);
            }
        } else if (key == 'r') {
            if (startCol == 7) {
                castles.put('k', false);
            } else if (startCol == 0) {
                castles.put('q', false);
            }
        }
        if (endKey == 'R') {
            if (endCol == 7) {
                castles.put('K', false);
            } else if (startCol == 0) {
                castles.put('Q', false);
            }
        } else if (endKey == 'r') {
            if (endCol == 7) {
                castles.put('k', false);
            } else if (endCol == 0) {
                castles.put('q', false);
            }
        }
        StringBuilder castle = new StringBuilder();
        for (Map.Entry<Character, Boolean> entry : castles.entrySet()) {
            if (entry.getValue()) {
                castle.append(entry.getKey());
            }
        }
        if (castle.isEmpty()) {
            return "-";
        } else {
            return castle.toString();
        }
    }

}
