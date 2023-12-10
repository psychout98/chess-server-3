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
    private String fenString;

    @JsonIgnore
    @BsonIgnore
    private FEN previousFEN;
    @JsonIgnore
    @BsonIgnore
    private char endKey;
    @JsonIgnore
    @BsonIgnore
    private boolean castleMove;
    @JsonIgnore
    @BsonIgnore
    private boolean enPassant;
    @JsonIgnore
    @BsonIgnore
    private boolean pushTwo;
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
    private boolean checkmate;
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
    private static final HashMap<String, String> castle = new HashMap<>();
    @JsonIgnore
    @BsonIgnore
    private static final double[] gradient = {0, 0.1, 0.2, 0.3, 0.3, 0.2, 0.1, 0};
    @JsonIgnore
    @BsonIgnore
    private static final HashMap<Character, Integer> pointValues = new HashMap<>();
    static {
        castle.put("0402", "q");
        castle.put("0406", "k");
        castle.put("7472", "Q");
        castle.put("7476", "K");
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
        pointValues.put('k', 0);
        pointValues.put('K', 0);
        pointValues.put('x', 0);
    }
    @JsonIgnore
    @BsonIgnore
    private static Random random = new Random();

    public Move(final char key, final int[] moveArray, final int[] enPassantTarget, final FEN previousFEN) {
        this.previousFEN = previousFEN;
        white = Character.isUpperCase(key);
        this.myMove = white == previousFEN.isWhiteToMove();
        this.key = key;
        enPassant = false;
        moveString = "";
        this.startRow = moveArray[0];
        this.startCol = moveArray[1];
        this.endRow = moveArray[2];
        this.endCol = moveArray[3];
        castleMove = false;
        pushTwo = false;
        checkmate = false;
        moveCode = String.format("%s%s%s%s", startRow, startCol, endRow, endCol);
        boolean pawnMove = key == 'p' || key == 'P';
        char[][] boardKey = Board.copyBoardKey(previousFEN.getBoardKey());
        endKey = boardKey[endRow][endCol];
        fenString = previousFEN.getFEN();
        boolean free = endKey == 'x';
        valid = !isObstructed(boardKey);
        moveString += pawnMove ? (startCol == endCol ? "" : (char) (startCol + 97)) : (white ? Character.toLowerCase(key) : key);
        if (pawnMove && isEnPassant(enPassantTarget)) {
            runEnPassant(boardKey, enPassantTarget);
        } else if (pawnMove && endRow == (white ? 0 : 7)) {
            runQueenPromotion(boardKey, free);
        } else if (pawnMove) {
            runBasicPawnMove(boardKey, free);
        } else if ((key == 'k' || key == 'K') && castle.get(moveCode) != null && previousFEN.getCastles().contains(castle.get(moveCode))) {
            runCastle(boardKey);
        } else if ((key == 'k' || key == 'K') && Math.abs(endCol - startCol) == 2) {
            valid = false;
            runBasicMove(boardKey, free);
        } else {
            runBasicMove(boardKey, free);
        }
        fenString = FEN.updateFEN(previousFEN, boardKey, key, endCol, pushTwo ? target() : "-");
        futures = new ArrayList<>();
        goodFutures = new ArrayList<>();
    }

    private String target() {
        return Board.spaceToSpace(new int[]{white ? startRow - 1 : startRow + 1, startCol});
    }

    private boolean isObstructed(char[][] boardKey) {
        boolean obstructed = false;
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
        if (Math.abs(endRow - startRow) == 2) {
            pushTwo = true;
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

    private void runEnPassant(char[][] boardKey, int[] enPassantTarget) {
        enPassant = true;
        moveString += "x";
        moveString += (char) (endCol + 97);
        moveString += (endRow + 1);
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[enPassantTarget[0]][enPassantTarget[1]] = 'x';
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

    public boolean isEnPassant(int[] enPassantTarget) {
        if (enPassantTarget == null) {
            return false;
        } else {
            boolean attack = startCol != endCol;
            return attack && endRow == enPassantTarget[0] && endCol == enPassantTarget[1];
        }
    }

    public void generateFutures() {
        Board copyBoard = Board.builder()
                .fenString(previousFEN.getFEN())
                .history(new ArrayList<>())
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
            checkmate = copyBoard.isCheckmate();
        } catch (InvalidMoveException e) {
            System.out.println(e.getMessage());
            valid = false;
        }
        futures = new ArrayList<>(copyBoard.getMoves().values());
        goodFutures = new ArrayList<>(futures.stream().filter(Move::isValid).filter(Move::isMyMove).toList());
    }

    private void calculateAdvantage(HashMap<String, Move> positionMap) {
        if (!goodFutures.isEmpty()) {
            goodFutures.forEach(future -> future.calculateAdvantage(positionMap));
            Move bestMove = findHighestAdvantage();
            if (bestMove == null || checkmate) {
                advantage = white ? 100 : -100;
            } else {
                advantage = bestMove.advantage + calculateStrategicAdvantage();
            }
        } else {
            advantage = calculateMaterialAdvantage(fenString);
        }
        positionMap.put(fenString, this);
    }

    private double calculateStrategicAdvantage() {
        double whitePossibilities = futures.stream()
                .filter(future -> future.valid && future.white)
                .mapToDouble(Move::calculateControl).sum();
        double blackPossibilities = futures.stream()
                .filter(future -> future.valid && !future.white)
                .mapToDouble(Move::calculateControl).sum();
        double kingQueenFactor = castleMove ? 3 : key == 'k' || key == 'K' || key == 'q' || key == 'Q' ? -1 : 0;

        return (white ? 1 : -1) * (kingQueenFactor) + (whitePossibilities - blackPossibilities);
    }

    private static double calculateControl(Move future) {
        double rowControl = gradient[future.endRow];
        double colControl = gradient[future.endCol];
        double attackValue = calculatePoints(future.endKey) / 10;
        return (future.white ? 1 : -1) * (rowControl + colControl) + attackValue;
    }

    private static double calculateMaterialAdvantage(String fenString) {
        double advantage = 0;
        String[] rows = fenString.split(" ")[0].split("/");
        for (String row : rows) {
            for (char key : row.toCharArray()) {
                advantage += calculatePoints(key);
            }
        }
        return advantage;
    }

    private static double calculatePoints(char key) {
        return Objects.requireNonNullElse(pointValues.get(key), 0);
    }

    public Move mapped(HashMap<String, Move> positionMap) {
        Move mappedPosition = positionMap.get(fenString);
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
                System.out.println(i + " " + sumNodes());
                positionMap = new HashMap<>();
                calculateMaterialAdvantage(fenString);
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
            System.out.println(maxDepth + " " + sumNodes());
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
            if (branchDepth < maxDepth - 2) {
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
