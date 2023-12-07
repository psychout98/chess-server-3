package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidKeyException;
import com.example.chessserver3.exception.InvalidMoveException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    @BsonIgnore
    private Map<String, Piece> pieces;
    @BsonIgnore
    private String[][] boardKey;
    private String boardKeyString;
    private boolean whiteToMove;
    private boolean check;
    private boolean checkmate;
    private boolean stalemate;
    private Map<String, Move> moves;
    private List<Move> history;
    @BsonIgnore
    @JsonIgnore
    private boolean shallow;
    private Castle castle;
    private int winner;

    public void resign(boolean white) {
        winner = white ? 2 : 1;
    }

    public void update() {
        pieces = new HashMap<>();
        moves = new HashMap<>();
        boardKey = boardKeyStringToArray(boardKeyString);
        addPieces();
        addMoves();
        check = checkCheck(whiteToMove);
        checkmate = moves.values().stream().filter(Move::isValid).collect(Collectors.toSet()).isEmpty();
        stalemate = (checkmate & !check) || isFiftyNeutral() || isThreeFoldRep();
        winner = stalemate ? 3 : (checkmate ? (whiteToMove ? 2 : 1) : 0);
    }


    public static String[][] boardKeyStringToArray(String boardKeyString) {
        String[][] boardKey = new String[8][8];
        String[] split = boardKeyString.split(",");
        for (int i=0; i<8; i++) {
            for (int j=0; j<8; j++) {
                String key = split[(8 * i) + j];
                boardKey[i][j] = Objects.equals(key, "x") ? "" : key;
            }
        }
        return boardKey;
    }

    public static String boardKeyArrayToString(String[][] boardKeyArray) {
        StringBuilder boardKeyString = new StringBuilder();
        for (int i=0; i<8; i++) {
            for (int j=0; j<8; j++) {
                String key = boardKeyArray[i][j];
                boardKeyString.append(key.isEmpty() ? "x," : key + ",");
            }
        }
        return boardKeyString.toString();
    }

    private void addPieces() {
        pieces = new HashMap<>();
        for (int i=0; i<8; i++) {
            for (int j=0; j<8; j++) {
                addPiece(boardKey[i][j], i, j);
            }
        }
        pieces.values().forEach(Piece::generateMoves);
    }

    private void addPiece(String key, int row, int col) {
        if (!key.isEmpty()) {
            char color = key.charAt(0);
            if (color == 'w') {
                addPiece(row, col, true, key);
            } else if (color == 'b') {
                addPiece(row, col, false, key);
            }
        }
    }

    private void addPiece(int row, int col, boolean white, String key) {
        char name = key.charAt(1);
        Piece piece = switch (name) {
            case 'p' -> new Pawn(row, col, white,this);
            case 'r' -> new Rook(row, col, white,this);
            case 'n' -> new Knight(row, col, white,this);
            case 'b' -> new Bishop(row, col, white,this);
            case 'k' -> new King(row, col, white,this);
            case 'q' -> new Queen(row, col, white,this);
            default -> throw new InvalidKeyException("Invalid piece key");
        };
        pieces.put(key, piece);
    }

    private void addMoves() {
        moves = pieces.values().stream()
                .map(Piece::getMoves)
                .flatMap(Set::stream)
                .map(moveCode -> new Move(whiteToMove, boardKeyString, history.get(history.size() - 1).getBoardKeyString(), moveCode, history.get(history.size() - 1), castle.copy(), getQueenIndex()))
                .collect(Collectors.toMap(Move::getMoveCode, Function.identity()));
        if (!shallow) {
            for (Move move : moves.values().stream().filter(Move::isValid).toList()) {
                move.generateFutures();
            }
        }
    }

    private int[] moveCodeToMove(String moveCode) {
        int[] move = new int[4];
        for (int i=0; i<4; i++) {
            move[i] = moveCode.charAt(i) - '0';
        }
        return move;
    }

    public boolean checkCheck(boolean white) {
        return moves.values().stream().filter(move -> move.isWhite() != white)
                .anyMatch(move -> keyAtSpace(move.getMove()[2], move.getMove()[3]).contains(white ? "wk" : "bk"));
    }


    public void move(String moveCode) {
        if (!shallow && winner != 0) {
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
        if (move != null && move.isValid() && move.isWhite() == whiteToMove) {
            boardKeyString = move.getBoardKeyString();
            history.add(move);
            whiteToMove = !whiteToMove;
            castle.checkCastles(move.getMovingPiece());
            shallow = true;
            update();
        } else {
            throw new InvalidMoveException("Invalid move: " + moveCode);
        }
    }

    public String keyAtSpace(int row, int col) {
        return boardKey[row][col];
    }


    private int getQueenIndex() {
        int index = 1;
        for (int i=0; i<8; i++) {
            for (int j=0; j<8; j++) {
                if (boardKey[i][j].contains("q")) {
                    index++;
                }
            }
        }
        return index;
    }

    private boolean isFiftyNeutral() {
        return history.size() > 100 && history.subList(history.size() - 100, history.size()).stream().noneMatch(move -> move.getMoveString().contains("x"));
    }

    private boolean isThreeFoldRep() {
        if (history.size() >= 9) {
            String boardKeyString = history.get(history.size() - 1).getBoardKeyString();
            for (int i=1; i<3; i++) {
                if (!Objects.equals(boardKeyString, history.get(history.size() - 1 - 4 * i).getBoardKeyString())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
