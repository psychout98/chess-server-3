package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidFENException;
import com.example.chessserver3.exception.InvalidMoveException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Board {

    @BsonId
    private String id;
    private Player white;
    private Player black;
    private String FEN;
    @BsonIgnore
    private char[][] boardKey;
    private boolean whiteToMove;
    private boolean check;
    private boolean checkmate;
    private boolean stalemate;
    private Map<String, Move> moves;
    private List<Move> history;
    private Castle castle;
    private int winner;
    @BsonIgnore
    @JsonIgnore
    @Builder.Default
    private boolean shallow = false;
    @BsonIgnore
    @JsonIgnore
    private final static Boolean[][] kingMoves = {{false,true,true,true,false},{true,true,false,true,true},{false,true,true,true,false}};
    @BsonIgnore
    @JsonIgnore
    private final static Boolean[][] whitePawnMoves = {{false,true,false},{true,true,true},{false,false,false}};
    @BsonIgnore
    @JsonIgnore
    private final static Boolean[][] blackPawnMoves = {{false,false,false},{true,true,true},{false,true,false}};
    @BsonIgnore
    @JsonIgnore
    private final static Boolean[][] knightMoves = {{false,true,false,true,false},{true,false,false,false,true},{false,false,false,false,false},{true,false,false,false,true},{false,true,false,true,false}};
    @BsonIgnore
    @JsonIgnore
    private final static Boolean[][] bishopMoves = new Boolean[15][15];
    @BsonIgnore
    @JsonIgnore
    private final static Boolean[][] rookMoves = new Boolean[15][15];
    @BsonIgnore
    @JsonIgnore
    private final static Boolean[][] queenMoves = new Boolean[15][15];
    @BsonIgnore
    @JsonIgnore
    private final static HashMap<Character, Boolean[][]> moveMaps = new HashMap<>();
    @BsonIgnore
    @JsonIgnore
    private final static HashMap<Character, int[]> pieceLocations = new HashMap<>();
    static {
        pieceLocations.put('k', new int[]{1,2});
        pieceLocations.put('K', new int[]{1,2});
        pieceLocations.put('p', new int[]{0,1});
        pieceLocations.put('P', new int[]{2,1});
        pieceLocations.put('n', new int[]{2,2});
        pieceLocations.put('N', new int[]{2,2});
        pieceLocations.put('b', new int[]{7,7});
        pieceLocations.put('B', new int[]{7,7});
        pieceLocations.put('r', new int[]{7,7});
        pieceLocations.put('R', new int[]{7,7});
        pieceLocations.put('q', new int[]{7,7});
        pieceLocations.put('Q', new int[]{7,7});
        for (int i=0; i<15; i++) {
            for (int j=0; j<15; j++) {
                rookMoves[i][j] = false;
                bishopMoves[i][j] = false;
                queenMoves[i][j] = false;
            }
        }
        for (int i=0;i<15;i++) {
            for (int j=0;j<15;j++) {
                if (i == 7 && j != 7) {
                    rookMoves[i][j] = true;
                    queenMoves[i][j] = true;
                }
                if (j == 7 && i != 7) {
                    rookMoves[i][j] = true;
                    queenMoves[i][j] = true;
                }
                if (i == j && i != 7) {
                    bishopMoves[i][j] = true;
                    queenMoves[i][j] = true;
                }
                if (i == 14 - j && i != 7) {
                    bishopMoves[i][j] = true;
                    queenMoves[i][j] = true;
                }
            }
        }
        moveMaps.put('k', kingMoves);
        moveMaps.put('K', kingMoves);
        moveMaps.put('p', blackPawnMoves);
        moveMaps.put('P', whitePawnMoves);
        moveMaps.put('n', knightMoves);
        moveMaps.put('N', knightMoves);
        moveMaps.put('b', bishopMoves);
        moveMaps.put('B', bishopMoves);
        moveMaps.put('r', rookMoves);
        moveMaps.put('R', rookMoves);
        moveMaps.put('q', queenMoves);
        moveMaps.put('Q', queenMoves);
    }

    public void resign(boolean white) {
        winner = white ? 2 : 1;
    }

    public void update() {
            moves = new HashMap<>();
            boardKey = FENtoBoardKey(FEN);
        if (winner == 0) {
            addMoves();
            check = checkCheck(whiteToMove);
            checkmate = moves.values().stream().filter(Move::isValid).filter(Move::isMyMove).toList().isEmpty();
            stalemate = (checkmate & !check) || isFiftyNeutral() || isThreeFoldRep();
            winner = stalemate ? 3 : (checkmate ? (whiteToMove ? 2 : 1) : 0);
        }
    }


    public static char[][] FENtoBoardKey(String FEN) {
        String validChars = "rRnNbBkKqQpP";
        char[][] boardKey = new char[8][8];
        String[] split = FEN.split("/");
        for (int i=0; i<8; i++) {
            int j=0;
            for (char c : split[i].toCharArray()) {
                if (validChars.contains(String.valueOf(c))) {
                    boardKey[i][j] = c;
                    j++;
                } else if (c > 47 && c < 58) {
                    for (int k=j; k<(j + c - 48); k++) {
                        boardKey[i][k] = 'x';
                    }
                    j += c - 48;
                } else {
                    throw new InvalidFENException("Invalid character \"" + c + "\" in FEN");
                }
            }
        }
        return boardKey;
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

//            4,4
//            2,2
//            0,1
//
//    0       (2,3)   0       1       0
//    1       0       0       0       1
//    0       0       (4,4)   0       0
//    1       0       0       0       1
//    0       1       0       1       0

    private void addMoves() {
        for (int i=0;i<8;i++) {
            for (int j=0;j<8;j++) {
                if (boardKey[i][j] != 'x') {
                    Boolean[][] moveMap = moveMaps.get(boardKey[i][j]);
                    for (int k = 0; k < moveMap.length; k++) {
                        for (int l = 0; l < moveMap[k].length; l++) {
                            if (moveMap[k][l]) {
                                int[] location = pieceLocations.get(boardKey[i][j]);
                                int row = i + (k - location[0]);
                                int col = j + (l - location[1]);
                                if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                                    int[] moveArray = {i, j, row, col};
                                    Move move = new Move(boardKey[i][j], whiteToMove, copyBoardKey(boardKey), moveArray, history.get(history.size() - 1), castle.copy());
                                    moves.put(move.getMoveCode(), move);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!shallow) {
            moves.values().stream().filter(Move::isValid).filter(Move::isMyMove).forEach(Move::generateFutures);
        }
    }

    private static char[][] copyBoardKey(char[][] boardKey) {
        char[][] copy = new char[8][8];
        for (int i=0;i<8;i++) {
            System.arraycopy(boardKey[i], 0, copy[i], 0, 8);
        }
        return copy;
    }

    private int[] moveCodeToMove(String moveCode) {
        int[] move = new int[4];
        for (int i=0; i<4; i++) {
            move[i] = moveCode.charAt(i) - '0';
        }
        return move;
    }

    public boolean checkCheck(boolean white) {
        return moves.values().stream().filter(move -> move.isWhite() != white).filter(Move::isValid)
                .anyMatch(move -> Objects.equals(boardKey[move.getMoveArray()[2]][move.getMoveArray()[3]], (white ? 'K' : 'k')));
    }



    public void move(String moveCode) {
        if (winner != 0) {
            throw new InvalidMoveException("Game is over");
        }
        if (moveCode.length() == 4) {
            if (!Arrays.stream(moveCodeToMove(moveCode)).allMatch(i -> i < 8 && i >= 0)) {
                throw new InvalidMoveException(String.format("Unable to parse move code %s", moveCode));
            }
        } else {
            throw new InvalidMoveException(String.format("Move code has incorrect format %s", moveCode));
        }
        Move move = moves.get(moveCode);
        if (move != null && move.isValid() && move.isMyMove()) {
            FEN = move.getFEN();
            history.add(move);
            whiteToMove = !whiteToMove;
            castle.checkCastles(move.getKey(), move.getMoveArray()[1]);
            update();
        } else {
            throw new InvalidMoveException("Invalid move: " + moveCode);
        }
    }

    private boolean isFiftyNeutral() {
        return history.size() > 100 && history.subList(history.size() - 100, history.size()).stream().noneMatch(move -> move.getMoveString().contains("x"));
    }

    private boolean isThreeFoldRep() {
        int i = 0;
        for (Move move : history) {
            if (Objects.equals(move.getFEN(), FEN)) {
                i++;
            }
            if (i > 2) {
                return true;
            }
        }
        return false;
    }
}
