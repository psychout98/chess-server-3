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
    private String position;

    @JsonIgnore
    @BsonIgnore
    private String previousFEN;
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
    private double materialAdvantage;
    @JsonIgnore
    @BsonIgnore
    private double strategicAdvantage;
    @JsonIgnore
    @BsonIgnore
    private double positionAdvantage;
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
    private static final HashMap<Character, Double> pointValues = new HashMap<>();
    static {
        castle.put("0402", "q");
        castle.put("0406", "k");
        castle.put("7472", "Q");
        castle.put("7476", "K");
        pointValues.put('q', -25.21);
        pointValues.put('Q', 25.21);
        pointValues.put('r', -12.70);
        pointValues.put('R', 12.70);
        pointValues.put('b', -8.36);
        pointValues.put('B', 8.36);
        pointValues.put('n', -8.17);
        pointValues.put('N', 8.17);
        pointValues.put('p', -1.98);
        pointValues.put('P', 1.98);
        pointValues.put('k', 0.0);
        pointValues.put('K', 0.0);
        pointValues.put('x', 0.0);
    }

    public Move(final char key, final int[] moveArray, final int[] enPassantTarget, final FEN previousFEN) {
        this.previousFEN = previousFEN.getFEN();
        white = Character.isUpperCase(key);
        myMove = white == previousFEN.isWhiteToMove();
        this.key = key;
        enPassant = false;
        checkmate = false;
        moveString = "";
        startRow = moveArray[0];
        startCol = moveArray[1];
        endRow = moveArray[2];
        endCol = moveArray[3];
        castleMove = false;
        pushTwo = false;
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
        position = FEN.getBoardField(fenString);
        futures = new ArrayList<>();
        goodFutures = new ArrayList<>();
        calculateMaterialAdvantage();
        calculatePositionAdvantage();
        strategicAdvantage = 0;
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
                .fenString(previousFEN)
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
        } catch (InvalidMoveException e) {
            System.out.println(e.getMessage());
            valid = false;
        }
        futures = new ArrayList<>(copyBoard.getMoves().values().stream().filter(Move::isValid).toList());
        goodFutures = new ArrayList<>(futures.stream().filter(Move::isMyMove).toList());
        strategicAdvantage = calculateStrategicAdvantage();
    }

    private void calculateAdvantage() {
        if (!goodFutures.isEmpty()) {
            goodFutures.forEach(Move::calculateAdvantage);
            Move bestMove = findHighestAdvantage();
            if (bestMove != null) {
                if (checkmate) {
                    materialAdvantage = white ? 100 : -100;
                    strategicAdvantage = white ? 100 : -100;
                    positionAdvantage = white ? 100 : -100;
                } else {
                    materialAdvantage = bestMove.materialAdvantage;
                    strategicAdvantage = bestMove.strategicAdvantage == 0 ? strategicAdvantage : bestMove.strategicAdvantage;
                    positionAdvantage = bestMove.positionAdvantage;
                }
            }
        }
    }

    private double calculateStrategicAdvantage() {
        double whiteAttacks = futures.stream()
                .filter(future -> future.valid && future.white)
                .mapToDouble(future -> calculatePoints(future.endKey)).sum();
        double blackAttacks = futures.stream()
                .filter(future -> future.valid && !future.white)
                .mapToDouble(future -> calculatePoints(future.endKey)).sum();
        double kingQueenFactor = castleMove ? 10 : key == 'k' || key == 'K' || key == 'q' || key == 'Q' ? -3 : 0;

        return (white ? 1 : -1) * (kingQueenFactor) - (whiteAttacks + blackAttacks) / 10;
    }

    private void calculatePositionAdvantage() {
        double rowControl = gradient[endRow];
        double colControl = gradient[endCol];
        positionAdvantage = (white ? 1 : -1) * (rowControl + colControl);
    }

    private void calculateMaterialAdvantage() {
        materialAdvantage = gradient[endRow] + gradient[endCol];
        String[] rows = fenString.split(" ")[0].split("/");
        for (String row : rows) {
            for (char key : row.toCharArray()) {
                materialAdvantage += calculatePoints(key);
            }
        }
    }

    private static double calculatePoints(char key) {
        return Objects.requireNonNullElse(pointValues.get(key), 0.0);
    }

    public void buildTree(int branchDepth, int maxDepth, HashMap<String, Move> positionMap) {
        if (branchDepth < maxDepth) {
            ListIterator<Move> iterator = goodFutures.listIterator();
            goodFutures = new ArrayList<>();
            while (iterator.hasNext()) {
                Move future = iterator.next();
                Move mappedPosition = positionMap.get(future.position);
                if (mappedPosition == null) {
                    if (future.goodFutures.isEmpty()) {
                        future.generateFutures();
                        if (future.valid) {
                            future.buildTree(branchDepth + 1, maxDepth, positionMap);
                        }
                    } else {
                        future.buildTree(branchDepth + 1, maxDepth, positionMap);
                    }
                    goodFutures.add(future);
                    positionMap.put(future.position, future);
                } else {
                    goodFutures.add(mappedPosition);
                }
            }
            if (goodFutures.isEmpty()) {
                checkmate = true;
            }
            futures.clear();
        }
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
                calculateAdvantage();
                int before = sumGoodFutures();
                pruneFutures(0, i, maxDepth);
                positionMap.clear();
                System.out.println(i + " " + sumGoodFutures() + " / " + before);
            }
            if (goodFutures.size() == 1) {
                return goodFutures.stream().findFirst().get();
            }
            buildTree(0, maxDepth, positionMap);
            System.out.println(maxDepth + " " + sumGoodFutures());
        } else {
            buildTree(0, maxDepth, positionMap);
        }
        if (goodFutures.isEmpty()) {
            return null;
        } else {
            calculateAdvantage();
            return findHighestAdvantage();
        }
    }

    int sumGoodFutures() {
        if (goodFutures.isEmpty()) {
            return 0;
        } else {
            return goodFutures.size() + goodFutures.stream().map(Move::sumGoodFutures).mapToInt(Integer::intValue).sum();
        }
    }

    private Move findHighestAdvantage() {
        if (goodFutures.isEmpty()) {
            return null;
        } else {
            return white ? goodFutures.stream().min(Comparator.comparing(Move::totalAdvantage)).get() :
                    goodFutures.stream().max(Comparator.comparing(Move::totalAdvantage)).get();
        }
    }

    private void pruneFutures(int branchDepth, int pruneDepth, int maxDepth) {
        if (!goodFutures.isEmpty()) {
            if (branchDepth < maxDepth - 2) {
                goodFutures.forEach(future -> future.pruneFutures(branchDepth + 1, pruneDepth, maxDepth));
            }
            Move bestFuture = findHighestAdvantage();
            if (bestFuture != null) {
                int i = 0;
                while (goodFutures.size() > maxDepth - pruneDepth + 2 && i < 50) {
                    double range = 10 * Math.pow(0.9, i);
                    goodFutures.removeIf(future -> white ? future.totalAdvantage() > bestFuture.totalAdvantage() + range :
                            future.totalAdvantage() < bestFuture.totalAdvantage() - range);
                    i++;
                }
            }
        }
    }

    public double totalAdvantage() {
        return materialAdvantage + strategicAdvantage + positionAdvantage;
    }

}
