package com.example.chessserver3.model.computer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Pieces {

    public static final HashMap<String, List<byte[]>> moves = new HashMap<>();
    private final static boolean[][] kingSpots = {{false,true,true,true,false},{true,true,false,true,true},{false,true,true,true,false}};
    private final static boolean[][] whitePawnSpots = {{false,true,false},{true,true,true},{false,false,false}};
    private final static boolean[][] blackPawnSpots = {{false,false,false},{true,true,true},{false,true,false}};
    private final static boolean[][] knightSpots = {{false,true,false,true,false},{true,false,false,false,true},{false,false,false,false,false},{true,false,false,false,true},{false,true,false,true,false}};
    private final static boolean[][] bishopSpots = new boolean[15][15];
    private final static boolean[][] rookSpots = new boolean[15][15];
    private final static boolean[][] queenSpots = new boolean[15][15];
    public final static HashMap<Character, byte[]> pieceLocations = new HashMap<>();
    static {
        pieceLocations.put('k', new byte[]{1,2});
        pieceLocations.put('K', new byte[]{1,2});
        pieceLocations.put('p', new byte[]{0,1});
        pieceLocations.put('P', new byte[]{2,1});
        pieceLocations.put('n', new byte[]{2,2});
        pieceLocations.put('N', new byte[]{2,2});
        pieceLocations.put('b', new byte[]{7,7});
        pieceLocations.put('B', new byte[]{7,7});
        pieceLocations.put('r', new byte[]{7,7});
        pieceLocations.put('R', new byte[]{7,7});
        pieceLocations.put('q', new byte[]{7,7});
        pieceLocations.put('Q', new byte[]{7,7});
        for (int i=0; i<15; i++) {
            for (int j=0; j<15; j++) {
                rookSpots[i][j] = false;
                bishopSpots[i][j] = false;
                queenSpots[i][j] = false;
            }
        }
        for (byte i=0;i<15;i++) {
            for (byte j=0;j<15;j++) {
                if (i == 7 && j != 7) {
                    rookSpots[i][j] = true;
                    queenSpots[i][j] = true;
                }
                if (j == 7 && i != 7) {
                    rookSpots[i][j] = true;
                    queenSpots[i][j] = true;
                }
                if (i == j && i != 7) {
                    bishopSpots[i][j] = true;
                    queenSpots[i][j] = true;
                }
                if (i == 14 - j && i != 7) {
                    bishopSpots[i][j] = true;
                    queenSpots[i][j] = true;
                }
            }
        }
    }

    static {
        for (byte i=0; i<8; i++) {
            for (byte j=0; j<8; j++) {
                generateMoves('k', kingSpots, i, j);
                generateMoves('K', kingSpots, i, j);
                generateMoves('q', queenSpots, i, j);
                generateMoves('Q', queenSpots, i, j);
                generateMoves('r', rookSpots, i, j);
                generateMoves('R', rookSpots, i, j);
                generateMoves('n', knightSpots, i, j);
                generateMoves('N', knightSpots, i, j);
                generateMoves('b', bishopSpots, i, j);
                generateMoves('B', bishopSpots, i, j);
                generateMoves('p', blackPawnSpots, i, j);
                generateMoves('P', whitePawnSpots, i, j);
            }
        }
    }

    public static void generateMoves(char key, boolean[][] spots, byte i, byte j) {
        List<byte[]> pieceMoves = new ArrayList<>();
        for (byte k = 0; k < spots.length; k++) {
            for (byte l = 0; l < spots[k].length; l++) {
                if (spots[k][l]) {
                    byte row = (byte) (i + (k - pieceLocations.get(key)[0]));
                    byte col = (byte) (j + (l - pieceLocations.get(key)[1]));
                    if (row >= 0 && row < 8 && col >= 0 && col < 8 && !(row == i && col == j)) {
                        byte[] moveArray = {i, j, row, col};
                        pieceMoves.add(moveArray);
                    }
                }
            }
        }
        moves.put(String.format("%c%x%x", key, i, j), pieceMoves);
    }
}
