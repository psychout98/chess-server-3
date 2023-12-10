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
    private boolean myMove;
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
    private List<Move> goodFutures;
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
        white = Character.isUpperCase(key);
        this.myMove = white == whiteToMove;
        this.key = key;
        enPassant = false;
        this.castle = castle;
        this.lastMove = lastMove;
        moveString = "";
        this.moveArray = moveArray;
        castleMove = false;
        moveCode = String.format("%s%s%s%s", moveArray[0], moveArray[1], moveArray[2], moveArray[3]);
        boolean pawnMove = key == 'p' || key == 'P';
        boolean free = boardKey[moveArray[2]][moveArray[3]] == 'x';
        valid = !isObstructed(boardKey);
        moveString += pawnMove ? (moveArray[1] == moveArray[3] ? "" : (char) (moveArray[1] + 97)) : (white ? Character.toLowerCase(key) : key);
        if (pawnMove && !(lastMove.key == 'x') && isEnPassant(moveArray, lastMove.moveArray, lastMove.key, white ? -1 : 1)) {
            runEnPassant(boardKey);
        } else if (pawnMove && moveArray[2] == (white ? 0 : 7)) {
            runQueenPromotion(boardKey, free);
        } else if (pawnMove) {
            runBasicPawnMove(boardKey, free);
        } else if ((key == 'k' || key == 'K') && Castle.isCastle(moveCode)) {
            runCastle(boardKey);
        } else if ((key == 'k' || key == 'K') && Math.abs(moveArray[3] - moveArray[1]) == 2) {
            valid = false;
            runBasicMove(boardKey, free);
        } else {
            runBasicMove(boardKey, free);
        }
        FEN = Board.boardKeyToFEN(boardKey);
        futures = new ArrayList<>();
        goodFutures = new ArrayList<>();
    }

    private boolean isObstructed(char[][] boardKey) {
        boolean obstructed = false;
        char endKey = boardKey[moveArray[2]][moveArray[3]];
        boolean open = endKey == 'x' || (Character.isLowerCase(key) != Character.isLowerCase(endKey));
        String kingsAndKnights = "kKnN";
        String queensAndBishops = "qQbB";
        String queensAndRooksAndPawns = "qQrRpP";
        if (kingsAndKnights.contains(String.valueOf(key))) {
            obstructed = !open;
        }
        if (queensAndBishops.contains(String.valueOf(key))) {
            obstructed = diagonalObstruction(boardKey) || !open;
        }
        if (queensAndRooksAndPawns.contains(String.valueOf(key))) {
            obstructed = obstructed || (straightObstruction(boardKey) || !open);
        }
        return obstructed;
    }

    private boolean diagonalObstruction(char[][] boardKey) {
        int vertical = moveArray[2] - moveArray[0];
        int horizontal = moveArray[3] - moveArray[1];
        if (Math.abs(vertical) != Math.abs(horizontal)) {
            return false;
        } else if (vertical > 0 && horizontal > 0) {
            for (int i=1; i<vertical; i++) {
                if (boardKey[moveArray[0] + i][moveArray[1] + i] != 'x') {
                    return true;
                }
            }
        } else if (vertical > 0 && horizontal < 0) {
            for (int i=1; i<vertical; i++) {
                if (boardKey[moveArray[0] + i][moveArray[1] - i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0 && horizontal > 0) {
            for (int i=-1; i>vertical; i--) {
                if (boardKey[moveArray[0] + i][moveArray[1] - i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0 && horizontal < 0) {
            for (int i=-1; i>vertical; i--) {
                if (boardKey[moveArray[0] + i][moveArray[1] + i] != 'x') {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean straightObstruction(char[][] boardKey) {
        int vertical = moveArray[2] - moveArray[0];
        int horizontal = moveArray[3] - moveArray[1];
        if (vertical != 0 && horizontal != 0) {
            return false;
        } else if (vertical == 0 && horizontal < 0) {
            for (int i=moveArray[3] + 1; i<moveArray[1]; i++) {
                if (boardKey[moveArray[0]][i] != 'x') {
                    return true;
                }
            }
        } else if (vertical == 0 && horizontal > 0){
            for (int i=moveArray[1] + 1; i<moveArray[3]; i++) {
                if (boardKey[moveArray[0]][i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0) {
            for (int i=moveArray[2] + 1; i<moveArray[0]; i++) {
                if (boardKey[i][moveArray[1]] != 'x') {
                    return true;
                }
            }
        } else {
            for (int i=moveArray[0] + 1; i<moveArray[2]; i++) {
                if (boardKey[i][moveArray[1]] != 'x') {
                    return true;
                }
            }
        }
        return false;
    }

    private void runBasicMove(char[][] boardKey, boolean free) {
        if (!free) {
            moveString += "x";
        }
        moveString += (char) (moveArray[3] + 97);
        moveString += 8 - moveArray[2];
        boardKey[moveArray[0]][moveArray[1]] = 'x';
        boardKey[moveArray[2]][moveArray[3]] = key;
    }

    private void runBasicPawnMove(char[][] boardKey, boolean free) {
        char endKey = boardKey[moveArray[2]][moveArray[3]];
        if (Math.abs(moveArray[2] - moveArray[0]) == 2) {
            valid = valid && endKey == 'x' && (white ? moveArray[0] == 6 : moveArray[0] == 1);
            runBasicMove(boardKey, free);
        } else if (moveArray[1] != moveArray[3]) {
            valid = valid && endKey != 'x' && Character.isLowerCase(key) != Character.isLowerCase(endKey);
            runBasicMove(boardKey, free);
        } else {
            valid = valid && endKey == 'x';
            runBasicMove(boardKey, free);
        }
    }

    private void runEnPassant(char[][] boardKey) {
        enPassant = true;
        moveString += "x";
        moveString += (char) (moveArray[3] + 97);
        moveString += (moveArray[2] + 1);
        boardKey[moveArray[0]][moveArray[1]] = 'x';
        boardKey[moveArray[2]][moveArray[3]] = key;
        boardKey[lastMove.moveArray[2]][lastMove.moveArray[3]] = 'x';
    }

    private void runQueenPromotion(char[][] boardKey, boolean free) {
        if (!free) {
            moveString += "x";
        }
        moveString += (char) (moveArray[3] + 97);
        moveString += (moveArray[2] + 1);
        boardKey[moveArray[0]][moveArray[1]] = 'x';
        boardKey[moveArray[2]][moveArray[3]] = white ? 'Q' : 'q';
    }

    private void runCastle(char[][] boardKey) {
        valid = valid && castle.getValidCastles().get(moveCode);
        for (int[] space : Castle.castleRoutes.get(moveCode)) {
            if (boardKey[space[0]][space[1]] != 'x') {
                valid = false;
                break;
            }
        }
        castleMove = true;
        moveString = Castle.castleMoveString.get(moveCode);
        int[] rookMove = Castle.castleRookMove.get(moveCode);
        boardKey[moveArray[0]][moveArray[1]] = 'x';
        boardKey[moveArray[2]][moveArray[3]] = key;
        boardKey[rookMove[0]][rookMove[1]] = 'x';
        boardKey[rookMove[2]][rookMove[3]] = white ? 'R' : 'r';
    }

    public void generateFutures() {
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
            valid = valid && copyBoard.getMoves().values().stream()
                    .filter(future -> future.valid && !future.myMove)
                    .noneMatch(future -> Arrays.stream(Castle.castleSpaces.get(moveCode))
                                .anyMatch(dest -> future.moveArray[2] == dest[0] && future.moveArray[3] == dest[1]));
        }
        try {
            copyBoard.move(moveCode);
            valid = valid && !copyBoard.checkCheck(white);
        } catch (InvalidMoveException e) {
            System.out.println(e.getMessage());
            valid = false;
        }
        futures = new ArrayList<>(copyBoard.getMoves().values());
        goodFutures = new ArrayList<>(futures.stream().filter(Move::isValid).filter(Move::isMyMove).toList());
    }


    public static boolean isEnPassant(int[] move, int[] lastMove, char lastMoveKey, int direction) {
        if (lastMove == null) {
            return false;
        } else {
            boolean lastMovePawn = lastMoveKey == 'p' || lastMoveKey == 'P';
            boolean lastMovePushTwo = lastMove[2] - lastMove[0] == -2 * direction;
            boolean lastMoveVulnerable = lastMove[2] + direction == move[2] && lastMove[3] == move[3];
            return lastMovePawn && lastMovePushTwo && lastMoveVulnerable;
        }
    }

    private void calculateAdvantage(HashMap<String, Move> positionMap) {
        if (!goodFutures.isEmpty()) {
            double whitePossibilities = futures.stream().filter(future -> future.valid && future.white).toList().size();
            double blackPossibilities = futures.stream().filter(future -> future.valid && !future.white).toList().size();
            double kingFactor = castleMove ? 1 : key == 'k' || key == 'K' ? -0.25 : 0;
            goodFutures.forEach(future -> future.calculateAdvantage(positionMap));
            Move bestMove = findHighestAdvantage();
            if (bestMove == null) {
                advantage = white ? 100 : -100;
            } else {
                advantage = bestMove.advantage + ((white ? 1 : -1) * (kingFactor)) + 0.01 * (whitePossibilities - blackPossibilities);
                futures.clear();
                goodFutures.clear();
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
        positionMap.put(FEN, this);
    }

    private static int calculatePoints(char key) {
        Integer pointValue = pointValues.get(key);
        return Objects.requireNonNullElse(pointValue, 0);
    }

    public Move mapped(HashMap<String, Move> positionMap) {
        Move mappedPosition = positionMap.get(FEN);
        if (mappedPosition != null && Objects.equals(mappedPosition.moveCode, moveCode)) {
            return mappedPosition;
        } else {
            return null;
        }
    }

    public void buildTree(int branchDepth, int maxDepth, HashMap<String, Move> positionMap) {
        if (branchDepth < maxDepth) {
            ListIterator<Move> iterator = goodFutures.listIterator();
            goodFutures = new ArrayList<>();
            while (iterator.hasNext()) {
                Move future = iterator.next();
                Move mappedPosition = future.mapped(positionMap);
                if (mappedPosition == null) {
                    if (future.goodFutures.isEmpty()) {
                        future.generateFutures();
                    }
                    if (future.valid) {
                        future.buildTree(branchDepth + 1, maxDepth, positionMap);
                        goodFutures.add(future);
                    }
                } else {
                    goodFutures.add(mappedPosition);
                }
            }
        }
        calculateAdvantage(positionMap);
    }

    public Move findBestFuture(int maxDepth) {
        HashMap<String, Move> positionMap = new HashMap<>();
        if (futures.isEmpty()) {
            generateFutures();
        }
        if (maxDepth > 2) {
            for (int i=2; i<maxDepth; i++) {
                if (goodFutures.size() == 1) {
                    return goodFutures.stream().findFirst().get();
                }
                buildTree(0, i, positionMap);
                positionMap = new HashMap<>();
                pruneFutures(0, i);
            }
            if (goodFutures.size() == 1) {
                return goodFutures.stream().findFirst().get();
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
        if (goodFutures.isEmpty()) {
            return null;
        } else {
            Move bestFuture = goodFutures.get(random.nextInt(getGoodFutures().size()));
            for (Move future : goodFutures) {
                if (white ? future.advantage < bestFuture.advantage : future.advantage > bestFuture.advantage) {
                    bestFuture = future;
                }
            }
            return bestFuture;
        }
    }

    private void pruneFutures(int branchDepth, int maxDepth) {
        if (!goodFutures.isEmpty()) {
            Move bestFuture = findHighestAdvantage();
            if (bestFuture != null) {
//                int before = goodFutures.size();
                int i = 1;
                while (goodFutures.size() > 5 && i < 20) {
                    double range = Math.pow(0.5, i);
                    goodFutures.removeIf(future -> white ? future.advantage > bestFuture.advantage + range : future.advantage < bestFuture.advantage - range);
                    i++;
                }
//                System.out.println("pruned " + (before - goodFutures.size()) + " of " + before + " branches");
            }
            if (branchDepth < maxDepth - 2) {
                goodFutures.forEach(future -> future.pruneFutures(branchDepth + 1, maxDepth));
            }
        }
    }

}
