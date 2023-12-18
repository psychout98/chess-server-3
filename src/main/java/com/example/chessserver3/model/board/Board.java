package com.example.chessserver3.model.board;

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
    private FEN fen;
    private List<PGN> history;
    private int winner;
    private boolean check;
    private boolean checkmate;
    private boolean stalemate;
    @BsonIgnore
    private Map<String, Move> moves;
    private String lastMoveCode;
    @BsonIgnore
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
    public final static HashMap<Character, Boolean[][]> moveMaps = new HashMap<>();
    @BsonIgnore
    @JsonIgnore
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
                rookMoves[i][j] = false;
                bishopMoves[i][j] = false;
                queenMoves[i][j] = false;
            }
        }
        for (byte i=0;i<15;i++) {
            for (byte j=0;j<15;j++) {
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

    public static byte[] spaceToSpace(String space) {
        return new byte[]{(byte) (56 - space.charAt(1)), (byte) (space.charAt(0) - 97)};
    }

    public static String spaceToSpace(byte[] space) {
        return String.format("%s%s", (char) (space[1] + 97), (char) (56 - space[0]));
    }

    public void resign(boolean white) {
        winner = white ? 2 : 1;
    }

    public void update() {
            moves = new HashMap<>();
        if (winner == 0) {
            addMoves();
            check = checkCheck(fen.isWhiteToMove());
            checkmate = moves.values().stream().filter(Move::isValid).filter(Move::isMyMove).toList().isEmpty();
            stalemate = (checkmate & !check) || isFiftyNeutral() || isThreeFoldRep();
            winner = stalemate ? 3 : (checkmate ? (fen.isWhiteToMove() ? 2 : 1) : 0);
        }
    }

    private void addMoves() {
        char[][] boardKey = fen.getBoardKey();
        for (byte i=0;i<8;i++) {
            for (byte j=0;j<8;j++) {
                if (boardKey[i][j] != 'x') {
                    Boolean[][] moveMap = moveMaps.get(boardKey[i][j]);
                    for (byte k = 0; k < moveMap.length; k++) {
                        for (byte l = 0; l < moveMap[k].length; l++) {
                            if (moveMap[k][l]) {
                                byte[] location = pieceLocations.get(boardKey[i][j]);
                                byte row = (byte) (i + (k - location[0]));
                                byte col = (byte) (j + (l - location[1]));
                                if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                                    byte[] moveArray = {i, j, row, col};
                                    Move move = new Move(moveArray, fen);
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

    public static char[][] copyBoardKey(char[][] boardKey) {
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
                .anyMatch(move -> Objects.equals(fen.getBoardKey()[move.getEndRow()][move.getEndCol()], (white ? 'K' : 'k')));
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
            history.add(new PGN(move.getMoveString(), move.getMoveCode(), fen.getFen()));
            lastMoveCode = moveCode;
            fen = new FEN(move.getFenString());
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
        for (PGN move : history) {
            if (Objects.equals(FEN.getBoardField(move.getFen()), FEN.getBoardField(fen.getFen()))) {
                i++;
            }
            if (i > 2) {
                return true;
            }
        }
        return false;
    }
}
