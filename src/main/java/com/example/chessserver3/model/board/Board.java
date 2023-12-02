package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidKeyException;
import com.example.chessserver3.exception.InvalidMoveException;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private boolean shallow;
    private HashMap<String, Boolean> castle;
    private int winner;
    @BsonIgnore
    private static final HashMap<String, String> castleRookMoveCode = new HashMap<>();
    @BsonIgnore
    public final static HashMap<String, String> castleMoveString = new HashMap<>();
    @BsonIgnore
    public final static HashMap<String, int[]> castleRookMove = new HashMap<>();
    static {
        castleRookMove.put("0402", new int[]{0, 0, 0, 3});
        castleRookMove.put("0406", new int[]{0, 7, 0, 5});
        castleRookMove.put("7472", new int[]{7, 0, 7, 3});
        castleRookMove.put("7476", new int[]{7, 7, 7, 5});
        castleRookMoveCode.put("0402", "0003");
        castleRookMoveCode.put("0406", "0705");
        castleRookMoveCode.put("7472", "7073");
        castleRookMoveCode.put("7476", "7775");
        castleMoveString.put("0402", "O-O-O");
        castleMoveString.put("0406", "O-O");
        castleMoveString.put("7472", "O-O-O");
        castleMoveString.put("7476", "O-O");
    }

    public void resign(boolean white) {
        winner = white ? 2 : 1;
    }

    public void update() {
        pieces = new HashMap<>();
        moves = new HashMap<>();
        winner = 0;
        boardKey = boardKeyStringToArray(boardKeyString);
        addPieces();
        check = checkCheck(whiteToMove);
        addMoves();
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

    public int calculateAdvantage() {
        int whitePoints = pieces.values().stream().filter(Piece::isWhite).flatMapToInt(piece -> IntStream.of(piece.getPoints())).sum();
        int blackPoints = pieces.values().stream().filter(piece -> !piece.isWhite()).flatMapToInt(piece -> IntStream.of(piece.getPoints())).sum();
        return checkmate ? whiteToMove ? -40 : 40 : whitePoints - blackPoints;
    }

    private void addPieces() {
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
                .map(moveCode -> new Move(boardKeyString, moveCode, history.get(history.size() - 1), castle, getQueenIndex()))
                .collect(Collectors.toMap(Move::getMoveCode, Function.identity()));
        if (!shallow) {
            validateMoves();
        }
    }

    public void validateMoves() {
        for (Move move : moves.values()) {
            move.validate(boardKeyString);
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
        return moves.values().stream().filter(Move::isValid)
                .anyMatch(move -> keyAtSpace(move.getToRow(), move.getToCol()).contains(white ? "wk" : "bk"));
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
        if (move != null && move.isValid()) {
            boardKeyString = move.getBoardKeyString();
            if (!shallow) {
                history.add(move);
            }
            whiteToMove = !whiteToMove;
            checkCastles(move.getMovingPiece());
            update();
        } else {
//            System.out.println(boardKeyString);
//            System.out.println(moves.values().stream().map(Move::getMoveCode).collect(Collectors.toSet()));
            throw new InvalidMoveException("Invalid move");
        }
    }

    public String keyAtSpace(int row, int col) {
        return boardKey[row][col];
    }

    private void checkCastles(String key) {
        switch (key) {
            case "wk" -> {
                castle.put("0402", false);
                castle.put("0406", false);
            }
            case "bk" -> {
                castle.put("7472", false);
                castle.put("7476", false);
            }
            case "wr1" -> castle.put("0402", false);
            case "wr2" -> castle.put("0406", false);
            case "br1" -> castle.put("7472", false);
            case "br2" -> castle.put("7476", false);
        }
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
