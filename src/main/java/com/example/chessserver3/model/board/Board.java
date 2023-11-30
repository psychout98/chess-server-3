package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidKeyException;
import com.example.chessserver3.exception.InvalidMoveException;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
public class Board {

    @BsonId
    private String id;
    private Player white;
    private Player black;
    @BsonIgnore
    private HashMap<String, Piece> pieces;
    @BsonIgnore
    private String[][] boardKey;
    private String boardKeyString;
    private boolean whiteToMove;
    private Integer currentMove;
    private boolean check;
    private boolean checkmate;
    private boolean stalemate;
    private List<Move> history;
    private boolean shallow;
    private HashMap<String, Boolean> castle;
    private int winner;
    @BsonIgnore
    private static final HashMap<String, String> castleRookMoveCode = new HashMap<>();
    @BsonIgnore
    private final static HashMap<String, String> castleMoveString = new HashMap<>();
    static {
        castleRookMoveCode.put("0402", "0003");
        castleRookMoveCode.put("0406", "0705");
        castleRookMoveCode.put("7472", "7073");
        castleRookMoveCode.put("7476", "7775");
        castleMoveString.put("0402", "O-O-O");
        castleMoveString.put("0406", "O-O");
        castleMoveString.put("7472", "O-O-O");
        castleMoveString.put("7476", "O-O");
    }

    public Board(Player white, Player black, String boardKeyString, Integer currentMove, List<Move> history, boolean shallow, boolean checkmate, boolean stalemate, HashMap<String, Boolean> castle) {
        this.id = new ObjectId().toHexString();
        this.white = white;
        this.black = black;
        this.boardKey = boardKeyStringToArray(boardKeyString);
        this.boardKeyString = boardKeyString;
        this.currentMove = currentMove;
        this.whiteToMove = currentMove % 2 == 0;
        this.history = history == null ? List.of(new Move("", "", boardKeyArrayToString(boardKey), new int[]{0, 0}, false)) : history;
        this.shallow = shallow;
        this.castle = castle;
        this.check = false;
        this.checkmate = checkmate;
        this.stalemate = stalemate;
        this.winner = 0;
        this.pieces = new HashMap<>();
        addPieces();
    }

    public void updateBoard() {
        boardKey = boardKeyStringToArray(boardKeyString);
        this.pieces = new HashMap<>();
        addPieces();
    }

    public String[][] boardKeyStringToArray(String boardKeyString) {
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

    private String boardKeyArrayToString(String[][] boardKeyArray) {
        StringBuilder boardKeyString = new StringBuilder();
        for (int i=0; i<8; i++) {
            for (int j=0; j<8; j++) {
                String key = boardKeyArray[i][j];
                boardKeyString.append(key.isEmpty() ? "x," : key + ",");
            }
        }
        return boardKeyString.toString();
    }

    public Board shallowCopy(int currentMove) {
        return new Board(null, null, boardKeyString, currentMove, List.copyOf(history), true, checkmate, stalemate, castle);
    }

    private void addPieces() {
        for (int i=0; i<8; i++) {
            for (int j=0; j<8; j++) {
                addPiece(boardKey[i][j], i, j);
            }
        }
        if (!checkmate) {
            pieces.values().forEach(piece -> piece.generateMoves(this));
//            pieces.values().parallelStream().forEach(piece -> piece.generateMoves(this));
        }
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
            case 'p' -> new Pawn(row, col, white, shallow);
            case 'r' -> new Rook(row, col, white, shallow);
            case 'n' -> new Knight(row, col, white, shallow);
            case 'b' -> new Bishop(row, col, white, shallow);
            case 'k' -> new King(row, col, white, shallow);
            case 'q' -> new Queen(row, col, white, shallow);
            default -> throw new InvalidKeyException("Invalid piece key");
        };
        pieces.put(key, piece);
    }

    public void validateKingMove(boolean white, int[] move) {
        Set<Piece> attackers = pieces.values().stream().filter(piece -> piece.isWhite() != white).collect(Collectors.toSet());
        Stream<Move> hotspots = attackers.stream().flatMap(piece -> piece.getMoves().stream().filter(Move::isAttack));
        if (hotspots.anyMatch(m -> Arrays.equals(m.getDestination(), move))) {
            throw new InvalidMoveException("King move to attacked square");
        }
    }

    public void move(String moveCode, boolean white) {
        move(moveCode, white,false);
    }

    public void resign(boolean white) {
        winner = white ? 2 : 1;
    }

    public void move(String moveCode, boolean white, boolean castleMove) {
        if (winner != 0) {
            throw new InvalidMoveException("Game is over");
        }
        if (white != whiteToMove) {
            throw new InvalidMoveException("It is " + (whiteToMove ? "white" : "black") + "'s turn");
        }
        if (moveCode.length() == 4) {
            int[] move = new int[4];
            for (int i=0; i<4; i++) {
                move[i] = moveCode.charAt(i) - '0';
            }
            if (Arrays.stream(move).allMatch(i -> i < 8 && i >= 0)) {
                move(moveCode, move, white, castleMove);
            } else {
                throw new InvalidMoveException(String.format("Unable to parse move code %s", moveCode));
            }
        } else {
            throw new InvalidMoveException(String.format("Move code has incorrect format %s", moveCode));
        }
    }

    private void move(String moveCode, int[] move, boolean white, boolean castleMove) {
        String key = boardKey[move[0]][move[1]];
        Piece piece = pieces.get(key);
        if (piece != null) {

            if (piece.isWhite() != whiteToMove) {
                throw new InvalidMoveException("It is " + (whiteToMove ? "white" : "black") + "'s turn");
            }
            String moveString = "";
            int[] destination = Arrays.copyOfRange(move, 2, 4);
            moveString += key.contains("p") ? (move[1] == move[3] ? "" : (char) (move[1] + 97)) : key.substring(1, 2);
            String takenPieceKey = boardKey[move[2]][move[3]];
            if (!takenPieceKey.isEmpty() && pieces.containsKey(takenPieceKey)) {
                if (key.contains("p") && isPromotable(whiteToMove, destination)) {
                    key = (piece.isWhite() ? "w" : "b") + "q" + getQueenIndex(piece.isWhite());
                    boardKey[move[0]][move[1]] = key;
                    pieces.put(key, new Queen(piece.getRow(), piece.getCol(), piece.isWhite(), piece.isShallow()));
                    piece = pieces.get(key);
                    piece.generateMoves(this);
                }
                pieces.remove(takenPieceKey);
                moveString += "x";
            } else if (key.contains("p")) {
                int direction = whiteToMove ? 1 : -1;
                if(isEnPassant(destination, direction)) {
                    takenPieceKey = boardKey[move[2] - direction][move[3]];
                    if (pieces.containsKey(takenPieceKey)) {
                        pieces.remove(takenPieceKey);
                        boardKey[move[2] - direction][move[3]] = "";
                        moveString += "x";
                    }
                }
                if (key.contains("p") && isPromotable(whiteToMove, destination)) {
                    key = (piece.isWhite() ? "w" : "b") + "q" + getQueenIndex(piece.isWhite());
                    boardKey[move[0]][move[1]] = key;
                    pieces.put(key, new Queen(piece.getRow(), piece.getCol(), piece.isWhite(), piece.isShallow()));
                    piece = pieces.get(key);
                    piece.generateMoves(this);
                }
            }
            piece.move(destination);

            if (key.contains("k") && Math.abs(move[3] - move[1]) == 2) {
                castle.forEach((castleKey, castleOk) -> {
                    if (Objects.equals(castleKey, moveCode) && castleOk) {
                        move(castleRookMoveCode.get(castleKey), white, true);
                    }
                });
            }

            moveString += (char) (move[3] + 97);
            moveString += (move[2] + 1);
            boardKey[move[2]][move[3]] = key;
            boardKey[move[0]][move[1]] = "";
            boardKeyString = boardKeyArrayToString(boardKey);

            if (castleMove) {
                return;
            }

            if (!shallow) {
                String castleNotation = castleMoveString.get(moveCode);
                history.add(new Move(moveCode, castleNotation == null ? moveString : castleNotation, boardKeyArrayToString(boardKey), destination, moveString.contains("x")));
                checkCastles(key);
            }

            pieces = new HashMap<>();
            checkStalemate();
            addPieces();
            Piece king = pieces.get(whiteToMove ? "wk" : "bk");
            validateKingMove(whiteToMove, new int[]{king.getRow(), king.getCol()});
            whiteToMove = !whiteToMove;
            currentMove++;

            if (!shallow) {
                checkmate = pieces.values().stream().noneMatch(p -> p.isWhite() == whiteToMove && !p.getMoves().isEmpty());
                try {
                    Board checkBoard = shallowCopy(currentMove);
                    king = pieces.get(whiteToMove ? "wk" : "bk");
                    checkBoard.validateKingMove(whiteToMove, new int[]{king.getRow(), king.getCol()});
                    check = false;
                } catch (InvalidMoveException e) {
                    check = true;
                }
            }

            winner = checkmate && !stalemate ? whiteToMove ? 2 : 1 : winner;

        } else {
            throw new InvalidMoveException("No piece at given start coordinate");
        }
    }

    private void pawnPromotion(int[] move, int[] destination, String key, Piece piece) {
        if (key.contains("p") && isPromotable(whiteToMove, destination)) {
            key = (piece.isWhite() ? "w" : "b") + "q" + getQueenIndex(piece.isWhite());
            boardKey[move[0]][move[1]] = key;
            pieces.put(key, new Queen(piece.getRow(), piece.getCol(), piece.isWhite(), piece.isShallow()));
            piece = pieces.get(key);
            piece.generateMoves(this);
        }
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

    public boolean isEnPassant(int[] move, int direction) {
        if (currentMove > 1) {
            String lastMoveCode = history.get(history.size() - 1).getMoveCode();
            int[] lastMove = {lastMoveCode.charAt(0) - '0', lastMoveCode.charAt(1) - '0', lastMoveCode.charAt(2) - '0', lastMoveCode.charAt(3) - '0'};
            boolean lastMovePawn = boardKey[lastMove[2]][lastMove[3]].contains("p");
            boolean lastMovePushTwo = lastMove[2] - lastMove[0] == -2 * direction;
            boolean lastMoveAttackable = lastMove[2] + direction == move[0] && lastMove[3] == move[1];
            return lastMovePawn && lastMovePushTwo && lastMoveAttackable;
        } else {
            return false;
        }
    }

    private boolean isPromotable(boolean white, int[] destination) {
        return destination[0] == (white ? 7 : 0);
    }

    private int getQueenIndex(boolean white) {
        int index = 1;
        for (int i=0; i<8; i++) {
            for (int j=0; j<8; j++) {
                String key = white ? "wq" : "bq";
                if (boardKey[i][j].contains(key)) {
                    index++;
                }
            }
        }
        return index;
    }

    private void checkStalemate() {
        stalemate = (checkmate && !check) || isFiftyNeutral() || isThreeFoldRep();
        winner = stalemate ? 3 : winner;
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
