package com.example.chessserver3.model.computer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Moves {

    public static final HashMap<String, List<MoveNode>> moves = new HashMap<>();
    private final static boolean[][] kingSpots = {{false,true,true,true,false},{true,true,false,true,true},{false,true,true,true,false}};
    private final static boolean[][] knightSpots = {{false,true,false,true,false},{true,false,false,false,true},{false,false,false,false,false},{true,false,false,false,true},{false,true,false,true,false}};
    public final static HashMap<Character, byte[]> pieceLocations = new HashMap<>();
    static {
        pieceLocations.put('k', new byte[]{1,2});
        pieceLocations.put('K', new byte[]{1,2});
        pieceLocations.put('p', new byte[]{0,1});
        pieceLocations.put('P', new byte[]{2,1});
        pieceLocations.put('n', new byte[]{2,2});
        pieceLocations.put('N', new byte[]{2,2});
        for (byte i=0; i<8; i++) {
            for (byte j=0; j<8; j++) {
                generateWhitePawnMoves(i, j);
                generateBlackPawnMoves(i, j);
                generateSingleMoves('k', i, j, kingSpots);
                generateSingleMoves('K', i, j, kingSpots);
                generateSingleMoves('n', i, j, knightSpots);
                generateSingleMoves('N', i, j, knightSpots);
                generateSlideMoves('b', i, j, new byte[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}});
                generateSlideMoves('B', i, j, new byte[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}});
                generateSlideMoves('r', i, j, new byte[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
                generateSlideMoves('R', i, j, new byte[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
                generateSlideMoves('q', i, j, new byte[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}});
                generateSlideMoves('Q', i, j, new byte[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}});
            }
        }
    }

    public static void generateWhitePawnMoves(byte i, byte j) {
        List<MoveNode> pieceMoves = new ArrayList<>();
        if (i > 0  && i < 7 && j > 0) {
            pieceMoves.add(new MoveNode(new byte[]{i, j, (byte) (i - 1), (byte) (j - 1)}, null));
        }
        if (i > 0 && i < 7 && j < 7) {
            pieceMoves.add(new MoveNode(new byte[]{i, j, (byte) (i - 1), (byte) (j + 1)}, null));
        }
        if (i > 0 && i < 7) {
            if (i == 6) {
                pieceMoves.add(new MoveNode(new byte[]{i, j, (byte) (i - 1), j},
                        new MoveNode(new byte[]{i, j, (byte) (i - 2), j}, null)));
            } else {
                pieceMoves.add(new MoveNode(new byte[]{i, j, (byte) (i - 1), j}, null));
            }
        }
        moves.put(String.format("P%x%x", i, j), pieceMoves);
    }

    public static void generateBlackPawnMoves(byte i, byte j) {
        List<MoveNode> pieceMoves = new ArrayList<>();
        if (i > 0  && i < 7 && j > 0) {
            pieceMoves.add(new MoveNode(new byte[]{i, j, (byte) (i + 1), (byte) (j - 1)}, null));
        }
        if (i > 0 && i < 7 && j < 7) {
            pieceMoves.add(new MoveNode(new byte[]{i, j, (byte) (i + 1), (byte) (j + 1)}, null));
        }
        if (i > 0 && i < 7) {
            if (i == 1) {
                pieceMoves.add(new MoveNode(new byte[]{i, j, (byte) (i + 1), j},
                        new MoveNode(new byte[]{i, j, (byte) (i + 2), j}, null)));
            } else {
                pieceMoves.add(new MoveNode(new byte[]{i, j, (byte) (i + 1), j}, null));
            }
        }
        moves.put(String.format("p%x%x", i, j), pieceMoves);
    }

    public static void generateSingleMoves(char key, byte i, byte j, boolean[][] spots) {
        List<MoveNode> pieceMoves = new ArrayList<>();
        for (byte k = 0; k < spots.length; k++) {
            for (byte l = 0; l < spots[k].length; l++) {
                if (spots[k][l]) {
                    byte row = (byte) (i + (k - pieceLocations.get(key)[0]));
                    byte col = (byte) (j + (l - pieceLocations.get(key)[1]));
                    if (row >= 0 && row < 8 && col >= 0 && col < 8 && !(row == i && col == j)) {
                        byte[] moveArray = {i, j, row, col};
                        pieceMoves.add(new MoveNode(moveArray, null));
                    }
                }
            }
        }
        moves.put(String.format("%c%x%x", key, i, j), pieceMoves);
    }

    public static void generateSlideMoves(char key, byte i, byte j, byte[][] deltas) {
        List<MoveNode> pieceMoves = new ArrayList<>();
        for (byte[] delta : deltas) {
            byte row = (byte) (i + delta[0]);
            byte col = (byte) (j + delta[1]);
            if (row >= 0 && row < 8 && col >= 0 && col < 8 && !(row == i && col == j)) {
                byte[] moveArray = {i, j, row, col};
                pieceMoves.add(new MoveNode(moveArray, delta[0], delta[1]));
            }
        }
        moves.put(String.format("%c%x%x", key, i, j), pieceMoves);
    }
}
