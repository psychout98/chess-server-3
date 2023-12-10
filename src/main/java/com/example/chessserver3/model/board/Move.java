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
    private boolean valid;
    private boolean white;
    private boolean myMove;
    private char key;
    private int startRow;
    private int endRow;
    private int startCol;
    private int endCol;
    private String FEN;

    @JsonIgnore
    @BsonIgnore
    private Move lastMove;
    @JsonIgnore
    @BsonIgnore
    private boolean castleMove;
    @JsonIgnore
    @BsonIgnore
    private boolean enPassant;
    @JsonIgnore
    @BsonIgnore
    private Castle castle;
    @JsonIgnore
    @BsonIgnore
    private List<Move> futures;
    @JsonIgnore
    @BsonIgnore
    private List<Move> goodFutures;
    @JsonIgnore
    @BsonIgnore
    private double advantage;
    @JsonIgnore
    @BsonIgnore
    private static final String queensAndRooksAndPawns = "qQrRpP";
    @JsonIgnore
    @BsonIgnore
    private static final String queensAndBishops = "qQbB";
    @JsonIgnore
    @BsonIgnore
    private static final String kingsAndKnights = "kKnN";
    @JsonIgnore
    @BsonIgnore
    private static final String kingsAndRooks = "kKrR";
    @JsonIgnore
    @BsonIgnore
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
    private static Random random = new Random();

    public Move(char key, boolean whiteToMove, char[][] boardKey, final int[] moveArray, final Move lastMove, final Castle castle) {
        white = Character.isUpperCase(key);
        this.myMove = white == whiteToMove;
        this.key = key;
        enPassant = false;
        this.castle = castle;
        this.lastMove = lastMove;
        moveString = "";
        this.startRow = moveArray[0];
        this.startCol = moveArray[1];
        this.endRow = moveArray[2];
        this.endCol = moveArray[3];
        castleMove = false;
        moveCode = String.format("%s%s%s%s", startRow, startCol, endRow, endCol);
        boolean pawnMove = key == 'p' || key == 'P';
        boolean free = boardKey[endRow][endCol] == 'x';
        valid = !isObstructed(boardKey);
        moveString += pawnMove ? (startCol == endCol ? "" : (char) (startCol + 97)) : (white ? Character.toLowerCase(key) : key);
        if (pawnMove && isEnPassant()) {
            runEnPassant(boardKey);
            System.out.println(moveCode + " " + valid);
        } else if (pawnMove && endRow == (white ? 0 : 7)) {
            runQueenPromotion(boardKey, free);
        } else if (pawnMove) {
            runBasicPawnMove(boardKey, free);
        } else if ((key == 'k' || key == 'K') && Castle.isCastle(moveCode)) {
            runCastle(boardKey);
        } else if ((key == 'k' || key == 'K') && Math.abs(endCol - startCol) == 2) {
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
        char endKey = boardKey[endRow][endCol];
        boolean open = endKey == 'x' || (Character.isLowerCase(key) != Character.isLowerCase(endKey));
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
        int vertical = endRow - startRow;
        int horizontal = endCol - startCol;
        if (Math.abs(vertical) != Math.abs(horizontal)) {
            return false;
        } else if (vertical > 0 && horizontal > 0) {
            for (int i=1; i<vertical; i++) {
                if (boardKey[startRow + i][startCol + i] != 'x') {
                    return true;
                }
            }
        } else if (vertical > 0 && horizontal < 0) {
            for (int i=1; i<vertical; i++) {
                if (boardKey[startRow + i][startCol - i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0 && horizontal > 0) {
            for (int i=-1; i>vertical; i--) {
                if (boardKey[startRow + i][startCol - i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0 && horizontal < 0) {
            for (int i=-1; i>vertical; i--) {
                if (boardKey[startRow + i][startCol + i] != 'x') {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean straightObstruction(char[][] boardKey) {
        int vertical = endRow - startRow;
        int horizontal = endCol - startCol;
        if (vertical != 0 && horizontal != 0) {
            return false;
        } else if (vertical == 0 && horizontal < 0) {
            for (int i=endCol + 1; i<startCol; i++) {
                if (boardKey[startRow][i] != 'x') {
                    return true;
                }
            }
        } else if (vertical == 0 && horizontal > 0){
            for (int i=startCol + 1; i<endCol; i++) {
                if (boardKey[startRow][i] != 'x') {
                    return true;
                }
            }
        } else if (vertical < 0) {
            for (int i=endRow + 1; i<startRow; i++) {
                if (boardKey[i][startCol] != 'x') {
                    return true;
                }
            }
        } else {
            for (int i=startRow + 1; i<endRow; i++) {
                if (boardKey[i][startCol] != 'x') {
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
        moveString += (char) (endCol + 97);
        moveString += 8 - endRow;
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
    }

    private void runBasicPawnMove(char[][] boardKey, boolean free) {
        char endKey = boardKey[endRow][endCol];
        if (Math.abs(endRow - startRow) == 2) {
            valid = valid && endKey == 'x' && (white ? startRow == 6 : startRow == 1);
            runBasicMove(boardKey, free);
        } else if (startCol != endCol) {
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
        moveString += (char) (endCol + 97);
        moveString += (endRow + 1);
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[lastMove.endRow][lastMove.endCol] = 'x';
    }

    private void runQueenPromotion(char[][] boardKey, boolean free) {
        if (!free) {
            moveString += "x";
        }
        moveString += (char) (endCol + 97);
        moveString += (endRow + 1);
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = white ? 'Q' : 'q';
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
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[rookMove[0]][rookMove[1]] = 'x';
        boardKey[rookMove[2]][rookMove[3]] = white ? 'R' : 'r';
    }

    public boolean isEnPassant() {
        if (lastMove.key == 'x') {
            return false;
        } else {
            boolean attack = startCol != endCol;
            boolean lastMovePawn = white ? lastMove.key == 'p' : lastMove.key == 'P';
            boolean lastMovePushTwo = lastMove.endRow - lastMove.startRow == (white ? 2 : -2);
            boolean lastMoveVulnerable = lastMove.endRow + (white ? -1 : 1) == endRow && lastMove.endCol == endCol;
            return attack && lastMovePawn && lastMovePushTwo && lastMoveVulnerable;
        }
    }

    public void generateFutures() {
        Board copyBoard = Board.builder()
                .FEN(lastMove.FEN)
                .history(new ArrayList<>(List.of(lastMove)))
                .whiteToMove(white)
                .castle(castle)
                .shallow(true)
                .build();
        copyBoard.update();
        if (castleMove) {
            valid = valid && copyBoard.getMoves().values().stream()
                    .filter(future -> future.valid && !future.myMove)
                    .noneMatch(future -> Arrays.stream(Castle.castleSpaces.get(moveCode))
                                .anyMatch(dest -> future.endRow == dest[0] && future.endCol == dest[1]));
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
//                futures.forEach(future -> future.futures.clear());
//                goodFutures.forEach(future -> future.goodFutures.clear());
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
        if (maxDepth > 1) {
            for (int i=1; i<maxDepth; i++) {
                if (goodFutures.size() == 1) {
                    return goodFutures.stream().findFirst().get();
                }
                buildTree(0, i, positionMap);
//                System.out.println(i + " " + sumNodes());
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
//            System.out.println(maxDepth + " " + sumNodes());
            return findHighestAdvantage();
        }
    }

    int sumNodes() {
        if (goodFutures.isEmpty()) {
            return 0;
        } else {
            return goodFutures.size() + goodFutures.stream().map(Move::sumNodes).mapToInt(Integer::intValue).sum();
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
            if (branchDepth < maxDepth) {
                goodFutures.forEach(future -> future.pruneFutures(branchDepth + 1, maxDepth));
            }
            Move bestFuture = findHighestAdvantage();
            if (bestFuture != null) {
                int i = 1;
                while (goodFutures.size() > 2 && i < 6) {
                    double range = 30 * Math.pow(0.3, i);
                    goodFutures.removeIf(future -> white ? future.advantage > bestFuture.advantage + range : future.advantage < bestFuture.advantage - range);
                    i++;
                }
            }
        }
    }

}
