package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Move {

    private String moveCode;
    private String moveString;
    @JsonIgnore
    private String boardKeyString;
    @JsonIgnore
    @BsonIgnore
    private String oldBoardKeyString;
    private boolean valid;
    @BsonIgnore
    @JsonIgnore
    private boolean white;
    @BsonIgnore
    @JsonIgnore
    private Move lastMove;
    @BsonIgnore
    @JsonIgnore
    private int[] move;
    @BsonIgnore
    @JsonIgnore
    private String movingPiece;
    @BsonIgnore
    @JsonIgnore
    private boolean castleMove;
    @BsonIgnore
    @JsonIgnore
    private boolean enPassant;
    @JsonIgnore
    private Castle castle;
    @BsonIgnore
    @JsonIgnore
    private List<Move> futures;
    @BsonIgnore
    @JsonIgnore
    private int advantage;
    @BsonIgnore
    @JsonIgnore
    private static final HashMap<Character, Integer> pointValues = new HashMap<>();
    static {
        pointValues.put('q', 9);
        pointValues.put('r', 5);
        pointValues.put('b', 3);
        pointValues.put('n', 3);
        pointValues.put('p', 1);
    }
    @BsonIgnore
    @JsonIgnore
    private static Random random = new Random();

    public Move(final boolean whiteToMove, final String boardKeyString, final String oldBoardKeyString, final String moveCode, final Move lastMove, final Castle castle, final int queenIndex) {
        this.oldBoardKeyString = oldBoardKeyString;
        this.valid = true;
        this.enPassant = false;
        this.moveCode = moveCode;
        this.castle = castle;
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
        this.white = startKey.startsWith("w");
        this.valid = white == whiteToMove;
        if (!lastMoveCode.isEmpty()) {
            lastMoveArray = moveCodeToMove(lastMoveCode);
            lastMoveKey = keyAtSpace(boardKey, lastMoveArray[2], lastMoveArray[3]);
        }
        this.moveString += startKey.contains("p") ? (move[1] == move[3] ? "" : (char) (move[1] + 97)) : startKey.substring(1, 2);
        if (startKey.contains("p") && !lastMoveCode.isEmpty() && isEnPassant(move, lastMoveArray, lastMoveKey, white ? 1 : 0)) {
            this.enPassant = true;
            this.moveString += "x";
            this.moveString += (char) (move[3] + 97);
            this.moveString += (move[2] + 1);
            boardKey[move[0]][move[1]] = "";
            boardKey[move[2]][move[3]] = startKey;
            boardKey[lastMoveArray[2]][lastMoveArray[3]] = "";
        } else if (startKey.contains("p") && move[2] == (white ? 7 : 0)) {
            if (!endKey.isEmpty()) {
                this.moveString += "x";
            }
            this.moveString += (char) (move[3] + 97);
            this.moveString += (move[2] + 1);
            boardKey[move[0]][move[1]] = "";
            boardKey[move[2]][move[3]] = (white ? "wq" : "bq") + queenIndex;
        } else if (startKey.contains("k") && Castle.isCastle(moveCode)) {
            this.valid = valid ? castle.getValidCastles().get(moveCode) : false;
            this.castleMove = true;
            this.moveString = Castle.castleMoveString.get(moveCode);
            int[] rookMove = Castle.castleRookMove.get(moveCode);
            String rookKey = keyAtSpace(boardKey, rookMove[0], rookMove[1]);
            boardKey[move[0]][move[1]] = "";
            boardKey[move[2]][move[3]] = startKey;
            boardKey[rookMove[0]][rookMove[1]] = "";
            boardKey[rookMove[2]][rookMove[3]] = rookKey;
        } else {
            if (!endKey.isEmpty()) {
                this.moveString += "x";
            } else if (startKey.contains("p") && move[1] != move[3]) {
                this.valid = false;
            }
            this.moveString += (char) (move[3] + 97);
            this.moveString += (move[2] + 1);
            boardKey[move[0]][move[1]] = "";
            boardKey[move[2]][move[3]] = startKey;
        }
        this.movingPiece = startKey;
        this.move = move;
        this.boardKeyString = Board.boardKeyArrayToString(boardKey);
        this.futures = new ArrayList<>();
    }

    public void generateFutures() {
        if (valid) {
            Castle copyCastle = castle.copy();
            Board copyBoard = Board.builder()
                    .boardKeyString(oldBoardKeyString)
                    .pieces(new HashMap<>())
                    .history(new ArrayList<>(List.of(lastMove)))
                    .whiteToMove(white)
                    .castle(copyCastle)
                    .shallow(true)
                    .build();
            copyBoard.update();
            if (castleMove) {
                valid = copyBoard.getMoves().values().stream()
                        .filter(m -> white != m.white)
                        .noneMatch(m -> Arrays.stream(Castle.castleSpaces.get(moveCode))
                                .anyMatch(dest -> m.move[2] == dest[0] && m.move[3] == dest[1]));
            }
            if (valid) {
                try {
                    copyBoard.move(moveCode);
                    valid = !copyBoard.checkCheck(white);
                } catch (InvalidMoveException e) {
                    System.out.println(e.getMessage());
                    valid = false;
                }
            }
            futures = new ArrayList<>(copyBoard.getMoves().values());
        } else {
            futures = Collections.emptyList();
        }
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
        boolean lastMoveVulnerable = lastMove[2] + direction == move[0] && lastMove[3] == move[1];
        return lastMovePawn && lastMovePushTwo && lastMoveVulnerable;
    }

    private void calculateAdvantage(int depth) {
        if (depth > 0) {
            Move bestFuture = findBestFuture(depth);
            if (bestFuture == null) {
                advantage = white ? 100 : -100;
            } else {
                advantage = bestFuture.advantage;
            }
        } else {
            advantage = 0;
            String[] boardKeys = boardKeyString.split(",");
            for (String key : boardKeys) {
                advantage += calculatePoints(key);
            }
        }
    }

    private static int calculatePoints(String key) {
        if (key.isEmpty() || key.contains("k") || key.contains("x")) {
            return 0;
        } else {
            int multiplier = key.startsWith("w") ? 1 : -1;
            try {
                Integer pointValue = pointValues.get(key.charAt(1));
                if (pointValue != null) {
                    return pointValue * multiplier;
                } else {
                    return 0;
                }
            } catch (StringIndexOutOfBoundsException e) {
                System.out.println(key);
                return 0;
            }
        }
    }

    public Move findBestFuture(int depth) {
        if (futures.isEmpty()) {
            generateFutures();
        }
        futures.forEach(Move::generateFutures);
        futures.removeIf(future -> !future.valid);
        if (futures.isEmpty()) {
            return null;
        } else {
            if (depth > 3) {
                futures.forEach(future -> future.calculateAdvantage(2));
                for (int i=3; i<depth; i++) {
                    pruneFutures();
                    for (Move future : futures) {
                        future.recalculateAdvantage(i);
                    }
                }
            } else {
                futures.forEach(future -> future.calculateAdvantage(depth - 1));
            }
            if (futures.isEmpty()) {
                return null;
            } else {
                return findHighestAdvantage();
            }
        }
    }

    private Move findHighestAdvantage() {
        if (futures.isEmpty()) {
            return null;
        } else {
            Move bestFuture = futures.get(random.nextInt(futures.size()));
            for (Move future : futures) {
                if (white ? future.advantage < bestFuture.advantage : future.advantage > bestFuture.advantage) {
//              System.out.println(white + " " + bestFuture.moveString + "(" + bestFuture.advantage + ")" + " " + future.moveString + "(" + future.advantage + ")");
                    bestFuture = future;
                }
            }
            return bestFuture;
        }
    }

    private void pruneFutures() {
        if (!futures.isEmpty()) {
            Move bestFuture = findHighestAdvantage();
            if (bestFuture != null) {
                futures.removeIf(future -> white ? future.advantage > bestFuture.advantage : future.advantage < bestFuture.advantage);
            }
        }
    }

    private void recalculateAdvantage(int depth) {
        if (depth > 1) {
            futures.forEach(future -> recalculateAdvantage(depth - 1));
            Move bestFuture = findHighestAdvantage();
            if (bestFuture == null) {
                advantage = white ? 100 : -100;
            } else {
                advantage = bestFuture.advantage;
            }
        } else {
            futures.forEach(future -> future.calculateAdvantage(1));
            futures.removeIf(future -> !future.valid);
        }
    }

}
