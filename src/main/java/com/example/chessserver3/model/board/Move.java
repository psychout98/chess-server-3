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
    private String FEN;
    @JsonIgnore
    @BsonIgnore
    private String oldFEN;
    private boolean valid;
    @BsonIgnore
    @JsonIgnore
    private boolean white;
    @BsonIgnore
    @JsonIgnore
    private Move lastMove;
    @BsonIgnore
    @JsonIgnore
    private int[] moveArray;
    @BsonIgnore
    @JsonIgnore
    private char key;
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
    private double advantage;


    @BsonIgnore
    @JsonIgnore
    private static final HashMap<Character, Integer> pointValues = new HashMap<>();
    static {
        pointValues.put('q', -9);
        pointValues.put('Q', 9);
        pointValues.put('r', -5);
        pointValues.put('R', 5);
        pointValues.put('b', -3);
        pointValues.put('B', 3);
        pointValues.put('n', -3);
        pointValues.put('N', 3);
        pointValues.put('p', -1);
        pointValues.put('P', 1);
        pointValues.put('k', -100);
        pointValues.put('K', 100);
        pointValues.put('x', 0);
    }
    @BsonIgnore
    @JsonIgnore
    private static Random random = new Random();

    public Move(char key, boolean whiteToMove, char[][] boardKey, final int[] moveArray, final Move lastMove, final Castle castle) {
        this.key = key;
        enPassant = false;
        this.castle = castle;
        this.lastMove = lastMove;
        moveString = "";
        this.moveArray = moveArray;
        castleMove = false;
        char endKey = boardKey[moveArray[2]][moveArray[3]];
        moveCode = String.format("%s%s%s%s", moveArray[0], moveArray[1], moveArray[2], moveArray[3]);
        white = key < 97;
        valid = white == whiteToMove;
        boolean pawnMove = key == 'p' || key == 'P';
        boolean free = endKey == 'x';
        boolean occupied = !free && (Character.isLowerCase(key) == Character.isLowerCase(endKey));
        moveString += pawnMove ? (moveArray[1] == moveArray[3] ? 0 : (char) (moveArray[1] + 97)) : (white ? key : key + 32);
        if (pawnMove && !(lastMove.key == 'x') && isEnPassant(moveArray, lastMove.moveArray, lastMove.key, white ? 1 : 0)) {
            enPassant = true;
            moveString += "x";
            moveString += (char) (moveArray[3] + 97);
            moveString += (moveArray[2] + 1);
            boardKey[moveArray[0]][moveArray[1]] = 'x';
            boardKey[moveArray[2]][moveArray[3]] = key;
            boardKey[lastMove.moveArray[2]][lastMove.moveArray[3]] = 'x';
        } else if (pawnMove && moveArray[2] == (white ? 7 : 0)) {
            if (!free) {
                moveString += "x";
            }
            moveString += (char) (moveArray[3] + 97);
            moveString += (moveArray[2] + 1);
            boardKey[moveArray[0]][moveArray[1]] = 'x';
            boardKey[moveArray[2]][moveArray[3]] = white ? 'Q' : 'q';
        } else if (key == 'k' || key == 'K' && Castle.isCastle(moveCode)) {
            valid = valid ? castle.getValidCastles().get(moveCode) : false;
            castleMove = true;
            moveString = Castle.castleMoveString.get(moveCode);
            int[] rookMove = Castle.castleRookMove.get(moveCode);
            boardKey[moveArray[0]][moveArray[1]] = 'x';
            boardKey[moveArray[2]][moveArray[3]] = key;
            boardKey[rookMove[0]][rookMove[1]] = 'x';
            boardKey[rookMove[2]][rookMove[3]] = white ? 'R' : 'r';
        } else {
            if (!free) {
                this.moveString += "x";
            } else if (pawnMove && moveArray[1] != moveArray[3]) {
                this.valid = false;
            }
            this.moveString += (char) (moveArray[3] + 97);
            this.moveString += (moveArray[2] + 1);
            boardKey[moveArray[0]][moveArray[1]] = 'x';
            boardKey[moveArray[2]][moveArray[3]] = key;
        }
        this.FEN = Board.boardKeyToFEN(boardKey);
        this.futures = new ArrayList<>();
    }

    public void generateFutures() {
        if (valid) {
            Castle copyCastle = castle.copy();
            Board copyBoard = Board.builder()
                    .FEN(lastMove.FEN)
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
                                .anyMatch(dest -> m.moveArray[2] == dest[0] && m.moveArray[3] == dest[1]));
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


    public static boolean isEnPassant(int[] move, int[] lastMove, char lastMoveKey, int direction) {
        boolean lastMovePawn = lastMoveKey == 'p' || lastMoveKey == 'P';
        boolean lastMovePushTwo = lastMove[2] - lastMove[0] == -2 * direction;
        boolean lastMoveVulnerable = lastMove[2] + direction == move[2] && lastMove[3] == move[3];
        return lastMovePawn && lastMovePushTwo && lastMoveVulnerable;
    }

    private void calculateAdvantage(int branchDepth, int maxDepth, HashMap<String, String> positionMap) {
        if (!futures.isEmpty() && branchDepth <= maxDepth) {
            double possibilityFactor = 0.01 * futures.size();
            double kingFactor = castleMove ? white ? 1 : -1 : key == 'k' ? 0.25 : key == 'K' ? 0.25 : 0;
            futures.forEach(future -> future.calculateAdvantage(branchDepth + 1, maxDepth, positionMap));
            Move bestMove = findHighestAdvantage();
            if (bestMove == null) {
                advantage = white ? 100 : -100;
            } else {
                advantage = bestMove.advantage + possibilityFactor + kingFactor;
                futures.forEach(future -> future.setFutures(new ArrayList<>()));
            }
        } else {
            advantage = 0;
            String[] rows = FEN.split("/");
            for (String row : rows) {
                for (char key : row.toCharArray()) {
                    advantage += calculatePoints(key);
                }
            }
        }
        positionMap.put(FEN, moveString);
    }

    private static int calculatePoints(char key) {
        Integer pointValue = pointValues.get(key);
        return Objects.requireNonNullElse(pointValue, 0);
    }

    public boolean mapped(HashMap<String, String> positionMap) {
        String mappedPosition = positionMap.get(FEN);
        if (mappedPosition != null) {
            return mappedPosition.equals(moveCode);
        } else {
            return false;
        }
    }

    public void buildTree(int branchDepth, int maxDepth, HashMap<String, String> positionMap) {
        if (branchDepth < maxDepth) {
            futures.removeIf(future -> future.mapped(positionMap));
            futures.forEach(Move::generateFutures);
            futures.removeIf(future -> !future.valid);
            futures.forEach(future -> future.buildTree(branchDepth + 1, maxDepth, positionMap));
            calculateAdvantage(branchDepth, maxDepth, positionMap);
            pruneFutures(branchDepth, maxDepth);
        }
    }

    public Move findBestFuture(int maxDepth) {
        HashMap<String, String> positionMap = new HashMap<>();
        if (futures.isEmpty()) {
            generateFutures();
        }
        if (maxDepth > 1) {
            for (int i=1; i<maxDepth; i++) {
                buildTree(0, i, positionMap);
                pruneFutures(0, i);
                if (futures.size() == 1) {
                    return futures.stream().findFirst().get();
                }
                positionMap = new HashMap<>();
            }
            buildTree(0, maxDepth, positionMap);
        } else {
            buildTree(0, maxDepth, positionMap);
        }
        if (futures.isEmpty()) {
            return null;
        } else {
            return findHighestAdvantage();
        }
    }

    private Move findHighestAdvantage() {
        if (futures.isEmpty()) {
            return null;
        } else {
            Move bestFuture = futures.get(random.nextInt(futures.size()));
            for (Move future : futures) {
                if (white ? future.advantage < bestFuture.advantage : future.advantage > bestFuture.advantage) {
                    bestFuture = future;
                }
            }
            return bestFuture;
        }
    }

    private void pruneFutures(int branchDepth, int maxDepth) {
        if (!futures.isEmpty() && branchDepth <= maxDepth) {
            if (branchDepth < maxDepth - 2) {
                futures.forEach(future -> future.pruneFutures(branchDepth + 1, maxDepth));
            }
            Move bestFuture = findHighestAdvantage();
            if (bestFuture != null) {
                int i = 1;
                while (futures.size() > 5 && i < 10) {
                    double range = Math.pow(0.5, i);
                    futures.removeIf(future -> white ? future.advantage > bestFuture.advantage + range : future.advantage < bestFuture.advantage - range);
                    i++;
                }
            }
        }
    }

}
