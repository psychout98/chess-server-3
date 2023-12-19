package com.example.chessserver3.model.computer;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class BitBoard {

    private boolean[][] bitMap = new boolean[8][8];

    public static BitBoard pieces(char[][] rows) {
        BitBoard pieces = new BitBoard();
        for (byte i=0; i<8; i++) {
            byte j = 0;
            for (char k : rows[i]) {
                if (Character.isDigit(k)) {
                    j += (byte) (k - 48);
                } else {
                    pieces.on(i, j);
                    j++;
                }
            }
        }
        return pieces;
    }

    public static BitBoard whitePieces(char[][] rows) {
        BitBoard whitePieces = new BitBoard();
        for (byte i=0; i<8; i++) {
            byte j = 0;
            for (char k : rows[i]) {
                if (Character.isDigit(k)) {
                    j += (byte) (k - 48);
                } else {
                    if (Character.isUpperCase(k)) {
                        whitePieces.on(i, j);
                    }
                    j++;
                }
            }
        }
        return whitePieces;
    }

    public static BitBoard blackPieces(char[][] rows) {
        BitBoard blackPieces = new BitBoard();
        for (byte i=0; i<8; i++) {
            byte j = 0;
            for (char k : rows[i]) {
                if (Character.isDigit(k)) {
                    j += (byte) (k - 48);
                } else {
                    if (Character.isLowerCase(k)) {
                        blackPieces.on(i, j);
                    }
                    j++;
                }
            }
        }
        return blackPieces;
    }

    private void on(byte i, byte j) {
        bitMap[i][j] = true;
    }

    private void off(byte i, byte j) {
        bitMap[i][j] = false;
    }

    private void allOn() {
        for (byte i=0; i<8; i++) {
            for (byte j=0; j<8; j++) {
                bitMap[i][j] = true;
            }
        }
    }

    private void allOff() {
        for (byte i=0; i<8; i++) {
            for (byte j=0; j<8; j++) {
                bitMap[i][j] = false;
            }
        }
    }

    public boolean get(byte i, byte j) {
        return bitMap[i][j];
    }

    private List<byte[]> getAllOn() {
        List<byte[]> spots = new ArrayList<>();
        for (byte i=0; i<8; i++) {
            for (byte j=0; j<8; j++) {
                if (bitMap[i][j]) {
                    spots.add(new byte[]{i, j});
                }
            }
        }
        return spots;
    }

    private List<byte[]> getAllOff() {
        List<byte[]> spots = new ArrayList<>();
        for (byte i=0; i<8; i++) {
            for (byte j=0; j<8; j++) {
                if (!bitMap[i][j]) {
                    spots.add(new byte[]{i, j});
                }
            }
        }
        return spots;
    }
}
