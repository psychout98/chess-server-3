package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Move {

    private String moveCode;
    private String moveString;
    private String boardKeyString;
    private boolean valid;
    @BsonIgnore
    @JsonIgnore
    private boolean white;
    @BsonIgnore
    @JsonIgnore
    private int depth;
    @BsonIgnore
    @JsonIgnore
    private Move lastMove;
    @BsonIgnore
    @JsonIgnore
    private String movingPiece;
    @BsonIgnore
    @JsonIgnore
    private int fromRow;
    @BsonIgnore
    @JsonIgnore
    private int toRow;
    @BsonIgnore
    @JsonIgnore
    private int fromCol;
    @BsonIgnore
    @JsonIgnore
    private int toCol;
    @BsonIgnore
    @JsonIgnore
    private boolean castleMove;
    @BsonIgnore
    @JsonIgnore
    private boolean kingKiller;
    @BsonIgnore
    @JsonIgnore
    private boolean attack;
    @BsonIgnore
    @JsonIgnore
    private int advantage = 0;
    @BsonIgnore
    @JsonIgnore
    private HashMap<String, Boolean> castle;
    @BsonIgnore
    @JsonIgnore
    private static final HashMap<String, int[][]> castleSpaces = new HashMap<>();
    static {
        castleSpaces.put("0402", new int[][]{{0, 2}, {0, 3}, {0, 4}});
        castleSpaces.put("0406", new int[][]{{0, 4}, {0, 5}, {0, 6}});
        castleSpaces.put("7472", new int[][]{{7, 2}, {7, 3}, {7, 4}});
        castleSpaces.put("7476", new int[][]{{7, 4}, {7, 5}, {7, 6}});
    }

    public Move(final String boardKeyString, final String moveCode, final Move lastMove, final HashMap<String, Boolean> castle, final int queenIndex) {
        this.valid = true;
        this.moveCode = moveCode;
        String[][] boardKey = Board.boardKeyStringToArray(boardKeyString);
        this.lastMove = lastMove;
        this.moveString = "";
        String lastMoveCode = lastMove.getMoveCode();
        String lastMoveKey = "";
        int[] lastMoveArray = {};
        this.castleMove = false;
        int[] move = moveCodeToMove(moveCode);
        String startKey = keyAtSpace(boardKey, move[0], move[1]);
        String endKey = keyAtSpace(boardKey, move[2], move[3]);
        this.attack = false;
        boolean white = startKey.startsWith("w");
        if (!lastMoveCode.isEmpty()) {
            lastMoveArray = moveCodeToMove(lastMoveCode);
            lastMoveKey = keyAtSpace(boardKey, lastMoveArray[2], lastMoveArray[3]);
        }
        this.moveString += startKey.contains("p") ? (move[1] == move[3] ? "" : (char) (move[1] + 97)) : startKey.substring(1, 2);
        if (startKey.contains("p") && !lastMoveCode.isEmpty() && isEnPassant(move, lastMoveArray, lastMoveKey, white ? 1 : 0)) {
            this.moveString += "x";
            this.moveString += (char) (move[3] + 97);
            this.moveString += (move[2] + 1);
            boardKey[move[0]][move[1]] = "";
            boardKey[move[2]][move[3]] = startKey;
            boardKey[lastMoveArray[2]][lastMoveArray[3]] = "";
        } else if (startKey.contains("p") && move[2] == (white ? 7 : 0)) {
            if (!endKey.isEmpty()) {
                this.moveString += "x";
                this.attack = true;
            }
            this.moveString += (char) (move[3] + 97);
            this.moveString += (move[2] + 1);
            boardKey[move[0]][move[1]] = "";
            boardKey[move[2]][move[3]] = (white ? "wq" : "bq") + queenIndex;
        } else if (startKey.contains("k") && castle.containsKey(moveCode)) {
            this.castleMove = true;
            this.moveString = Board.castleMoveString.get(moveCode);
            int[] rookMove = Board.castleRookMove.get(moveCode);
            String rookKey = keyAtSpace(boardKey, rookMove[0], rookMove[1]);
            boardKey[move[0]][move[1]] = "";
            boardKey[move[2]][move[3]] = startKey;
            boardKey[rookMove[0]][rookMove[1]] = "";
            boardKey[rookMove[2]][rookMove[3]] = rookKey;
        } else {
            if (!endKey.isEmpty()) {
                this.moveString += "x";
                this.attack = true;
            } else if (startKey.contains("p") && move[1] != move[3]) {
                valid = false;
            }
            this.moveString += (char) (move[3] + 97);
            this.moveString += (move[2] + 1);
            boardKey[move[0]][move[1]] = "";
            boardKey[move[2]][move[3]] = startKey;
        }
        this.movingPiece = startKey;
        this.fromRow = move[0];
        this.fromCol = move[1];
        this.toRow = move[2];
        this.toCol = move[3];
        this.kingKiller = endKey.contains("k");
        this.castle = castle;
        this.white = startKey.startsWith("w");
        this.boardKeyString = Board.boardKeyArrayToString(boardKey);
    }

    public List<Move> validate(String oldBoardKeyString, boolean whiteToMove, boolean shallow) {
        Board copyBoard = Board.builder().moves(Collections.emptyMap()).build();
        if (white != whiteToMove) {
            valid = false;
        } else {
            copyBoard = Board.builder()
                    .boardKeyString(oldBoardKeyString)
                    .pieces(new HashMap<>())
                    .history(new ArrayList<>(List.of(lastMove)))
                    .whiteToMove(whiteToMove)
                    .castle(new HashMap<>(castle))
                    .shallow(shallow)
                    .build();
            copyBoard.update();
            if (castleMove) {
                valid = copyBoard.getMoves().values().stream()
                        .filter(move -> white != move.isWhite())
                        .anyMatch(move -> Arrays.stream(castleSpaces.get(moveCode))
                                .anyMatch(dest -> move.toRow == dest[0] && move.toCol == dest[1]));
            }
            try {
                copyBoard.move(moveCode);
                advantage = copyBoard.calculateAdvantage();
                valid = !copyBoard.checkCheck(white);
//                System.out.println(advantage + " " + valid);
            } catch (InvalidMoveException e) {
                valid = false;
            }
        }
        return valid ? copyBoard.getMoves().values().stream().toList() : Collections.emptyList();
    }


    private static int[] moveCodeToMove(final String moveCode) {
        int[] move = new int[4];
        for (int i=0; i<4; i++) {
            move[i] = moveCode.charAt(i) - '0';
        }
        return move;
    }

    private static String keyAtSpace(String[][] boardKey, int row, int col) {
        return boardKey[row][col];
    }


    public static boolean isEnPassant(int[] move, int[] lastMove, String lastMoveKey, int direction) {
        boolean lastMovePawn = lastMoveKey.contains("p");
        boolean lastMovePushTwo = lastMove[2] - lastMove[0] == -2 * direction;
        boolean lastMoveAttackable = lastMove[2] + direction == move[0] && lastMove[3] == move[1];
        return lastMovePawn && lastMovePushTwo && lastMoveAttackable;
    }

}
