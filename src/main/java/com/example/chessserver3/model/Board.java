package com.example.chessserver3.model;

import com.example.chessserver3.exception.InvalidKeyException;
import com.example.chessserver3.exception.InvalidMoveException;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonExtraElements;
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

    public Board(Player white, Player black, String boardKeyString, Integer currentMove, List<Move> history, boolean shallow, boolean checkmate, boolean stalemate) {
        this.id = new ObjectId().toHexString();
        this.white = white;
        this.black = black;
        this.boardKey = boardKeyStringToArray(boardKeyString);
        this.boardKeyString = boardKeyString;
        this.currentMove = currentMove;
        this.whiteToMove = currentMove % 2 == 0;
        this.history = history == null ? List.of(new Move("", "", boardKeyArrayToString(boardKey))) : history;
        this.shallow = shallow;
        this.check = false;
        this.checkmate = checkmate;
        this.stalemate = stalemate;
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

    public Board shallowCopy() {
        return new Board(null, null, boardKeyString, history.size() - 1, List.copyOf(history), true, checkmate, stalemate);
    }

    private void addPieces() {
        for (int i=0; i<8; i++) {
            for (int j=0; j<8; j++) {
                addPiece(boardKey[i][j], i, j);
            }
        }
        if (!checkmate) {
            pieces.forEach((key, value) -> value.generateMoves(this));
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
        Stream<int[]> hotspots = attackers.stream().flatMap(piece -> piece.getMoves().stream());
        if (hotspots.anyMatch(m -> Arrays.equals(m, move))) {
            throw new InvalidMoveException("King move to attacked square");
        }
    }

    public void move(String moveCode) {
        if (checkmate || stalemate) {
            throw new InvalidMoveException("Game is over");
        }
        String moveString = "";
        if (moveCode.length() == 4) {
            int[] move = new int[4];
            for (int i=0; i<4; i++) {
                move[i] = moveCode.charAt(i) - '0';
            }
            if (Arrays.stream(move).allMatch(i -> i < 8 && i >= 0)) {
                String key = boardKey[move[0]][move[1]];
                Piece piece = pieces.get(key);
                if (piece != null) {
                    if (piece.isWhite() != whiteToMove) {
                        throw new InvalidMoveException("It is " + (whiteToMove ? "white" : "black") + "'s turn");
                    }
                    int[] destination = Arrays.copyOfRange(move, 2, 4);
                    moveString += key.contains("p") ? "" : key.substring(1, 2);
                    String takenPieceKey = boardKey[move[2]][move[3]];
                    if (pieces.containsKey(takenPieceKey)) {
                        pieces.remove(takenPieceKey);
                        moveString += "x";
                    } else if (key.contains("p")) {
                        int direction = whiteToMove ? 1 : -1;
                        if(enPassantable(destination, direction)) {
                            takenPieceKey = boardKey[move[2] - direction][move[3]];
                            if (pieces.containsKey(takenPieceKey)) {
                                pieces.remove(takenPieceKey);
                                boardKey[move[2] - direction][move[3]] = "";
                                moveString += "x";
                            }
                        }
                    }
                    piece.move(destination);
                    char col = (char) (move[3] + 97);
                    moveString += col;
                    moveString += (move[2] + 1);
                    boardKey[move[2]][move[3]] = boardKey[move[0]][move[1]];
                    boardKey[move[0]][move[1]] = "";
                    boardKeyString = boardKeyArrayToString(boardKey);
                    if (!shallow) {
                        history.add(new Move(moveCode, moveString, boardKeyArrayToString(boardKey)));
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
                            king = pieces.get(whiteToMove ? "wk" : "bk");
                            validateKingMove(whiteToMove, new int[]{king.getRow(), king.getCol()});
                            check = false;
                        } catch (InvalidMoveException e) {
                            check = true;
                        }
                    }
                } else {
                    throw new InvalidMoveException("No piece at given start coordinate");
                }
            } else {
                throw new InvalidMoveException("Unable to parse move code");
            }
        } else {
            throw new InvalidMoveException("Move code has incorrect format");
        }
    }

    public boolean enPassantable(int[] move, int direction) {
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

    private void checkStalemate() {
        stalemate = (checkmate && !check) || isFiftyNeutral() || isThreeFoldRep();
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
