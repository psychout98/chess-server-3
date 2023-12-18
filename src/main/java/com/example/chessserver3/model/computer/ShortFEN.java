package com.example.chessserver3.model.computer;

import com.example.chessserver3.exception.InvalidFENException;
import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.FEN;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class ShortFEN {

    private final String fen;
    private char[][] boardKey = new char[8][8];
    private String boardCode;
    private boolean whiteToMove;
    private String castles;
    private byte[] enPassantTarget;

    public void build() {
        String[] fields = fen.split(" ");
        boardCode = fields[0];
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

    public ShortFEN(String fen) {
        this.fen = fen;
        build();
    }

    public void loadBoardKey(String boardField) {
        String[] splitBoard = boardField.split("/");
        if (splitBoard.length == 8) {
            for (int i = 0; i < 8; i++) {
                int j = 0;
                for (char c : splitBoard[i].toCharArray()) {
                    if (FEN.validChars.contains(String.valueOf(c))) {
                        boardKey[i][j] = c;
                        j++;
                    } else if (c > 47 && c < 58) {
                        for (int k = j; k < (j + c - 48); k++) {
                            boardKey[i][k] = 'x';
                        }
                        j += c - 48;
                    } else {
                        throw new InvalidFENException("Invalid character \"" + c + "\" in FEN");
                    }
                }
            }
        } else {
            throw new InvalidFENException("Invalid position \"" + boardField + "\" in FEN");
        }
    }

    public static String boardKeyToFEN(char[][] boardKey) {
        StringBuilder FEN = new StringBuilder();
        for (int i=0; i<8; i++) {
            int k = 0;
            for (int j=0; j<8; j++) {
                char key = boardKey[i][j];
                if (key == 'x' && j < 7) {
                    k++;
                } else if (key == 'x') {
                    k++;
                    FEN.append(k);
                } else {
                    if (k > 0) {
                        FEN.append(k);
                        k = 0;
                    }
                    FEN.append(key);
                }
            }
            if (i < 7) {
                FEN.append("/");
            }
        }
        return FEN.toString();
    }

    public static ShortFEN updateFEN(ShortFEN previousShortFEN, char[][] boardKey, char key, int endCol, String enPassantTarget) {
        return new ShortFEN(boardKeyToFEN(boardKey) +
                " " +
                (previousShortFEN.whiteToMove ? "b" : "w") +
                " " +
                updateCastle(previousShortFEN.castles, key, endCol) +
                " " +
                enPassantTarget);
    }

    public static String updateCastle(String oldCastle, char key, int startCol) {
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
