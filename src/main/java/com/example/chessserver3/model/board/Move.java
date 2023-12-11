package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"myMove", "key", "white", "startRow", "endRow", "startCol", "endCol",
        "fenString", "position", "previousFen", "endKey", "castleMove", "enPassant", "pushTwo",
        "futures", "materialAdvantage", "strategicAdvantage", "positionAdvantage",
        "checkmate", "queensAndRooksAndPawns", "queensAndBishops", "kingsAndKnights", "castle",
        "gradient", "pointValues", "rt"})
public class Move {

    private String moveCode;
    private String moveString;
    private boolean valid;
    private boolean myMove;
    private char key;
    private boolean white;
    private int startRow;
    private int endRow;
    private int startCol;
    private int endCol;
    private String fenString;
    private String position;
    private FEN previousFen;
    private char endKey;
    private boolean castleMove;
    private boolean enPassant;
    private boolean pushTwo;
    private List<Move> futures;
    private double materialAdvantage;
    private double strategicAdvantage;
    private double positionAdvantage;
    private boolean checkmate;
    private static final String queensAndRooksAndPawns = "qQrRpP";
    private static final String queensAndBishops = "qQbB";
    private static final String kingsAndKnights = "kKnN";
    private static final HashMap<String, String> castle = new HashMap<>();
    private static final double[] gradient = {0, 0.1, 0.2, 0.3, 0.3, 0.2, 0.1, 0};
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
        pointValues.put('k', -30.0);
        pointValues.put('K', 30.0);
        pointValues.put('x', 0.0);
    }
    private static final Runtime rt = Runtime.getRuntime();

    public Move(final char key, final int[] moveArray, final FEN previousFen) {
        this.previousFen = previousFen;
        white = Character.isUpperCase(key);
        myMove = white == previousFen.isWhiteToMove();
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
        char[][] boardKey = Board.copyBoardKey(previousFen.getBoardKey());
        endKey = boardKey[endRow][endCol];
        fenString = previousFen.getFen();
        boolean free = endKey == 'x';
        valid = !isObstructed(boardKey);
        moveString += pawnMove ? (startCol == endCol ? "" : (char) (startCol + 97)) : (white ? Character.toLowerCase(key) : key);
        if (pawnMove) {
            runPawnMove(boardKey, free, previousFen.getEnPassantTarget());
        } else if ((key == 'k' || key == 'K') && castle.get(moveCode) != null && previousFen.getCastles().contains(castle.get(moveCode))) {
            runCastle(boardKey);
        } else if ((key == 'k' || key == 'K') && Math.abs(endCol - startCol) == 2) {
            valid = false;
            runBasicMove(boardKey, free);
        } else {
            runBasicMove(boardKey, free);
        }
        fenString = FEN.updateFEN(previousFen, boardKey, key, endCol, pushTwo ? enPassantTarget() : "-");
        position = FEN.getBoardField(fenString);
        futures = new ArrayList<>();
        futures = new ArrayList<>();
        calculateMaterialAdvantage();
        calculatePositionAdvantage();
        strategicAdvantage = 0;
    }

    private String enPassantTarget() {
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

    private void runPawnMove(char[][] boardKey, boolean free, int[] enPassantTarget) {
        if (Math.abs(endRow - startRow) == 2) {
            pushTwo = true;
            valid = valid && endKey == 'x' && (white ? startRow == 6 : startRow == 1);
            runBasicMove(boardKey, free);
        } else if (startCol != endCol) {
            if (isEnPassant(enPassantTarget)) {
                runEnPassant(boardKey);
            } else {
                valid = valid && endKey != 'x' && Character.isLowerCase(key) != Character.isLowerCase(endKey);
                if (endRow == (white ? 0 : 7)) {
                    runQueenPromotion(boardKey, free);
                } else {
                    runBasicMove(boardKey, free);
                }
            }
        } else {
            valid = valid && endKey == 'x';
            if (endRow == (white ? 0 : 7)) {
                runQueenPromotion(boardKey, free);
            } else {
                runBasicMove(boardKey, free);
            }
        }
    }

    private void runEnPassant(char[][] boardKey) {
        int targetRow = white ? endRow + 1 : endRow - 1;
        enPassant = true;
        moveString += "x";
        moveString += (char) (endCol + 97);
        moveString += (targetRow + 1);
        boardKey[startRow][startCol] = 'x';
        boardKey[endRow][endCol] = key;
        boardKey[targetRow][endCol] = 'x';
    }

    private void runQueenPromotion(char[][] boardKey, boolean free) {
        if (!free) {
            moveString += "x";
        }
        moveString += (char) (endCol + 97);
        moveString += 8 - endRow;
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
                .fen(previousFen)
                .history(new ArrayList<>())
                .shallow(true)
                .build();
        copyBoard.update();
        try {
            copyBoard.move(moveCode);
            if (castleMove) {
                valid = valid && copyBoard.getMoves().values().stream()
                        .filter(future -> future.valid && future.myMove)
                        .noneMatch(future -> Arrays.stream(Castle.castleSpaces.get(moveCode))
                                .anyMatch(dest -> future.endRow == dest[0] && future.endCol == dest[1]));
            } else {
                valid = valid && !copyBoard.checkCheck(white);
            }
        } catch (InvalidMoveException e) {
            System.out.println(e.getMessage());
            valid = false;
        }
        futures = new ArrayList<>(copyBoard.getMoves().values().stream().filter(Move::isValid).toList());
        strategicAdvantage = calculateStrategicAdvantage();
        futures.removeIf(future -> !future.myMove);
    }

    private void calculateAdvantage() {
        if (!futures.isEmpty()) {
            futures.forEach(Move::calculateAdvantage);
            Move bestMove = findHighestAdvantage();
            if (bestMove != null) {
                if (checkmate) {
                    materialAdvantage = white ? 100 : -100;
                } else {
                    materialAdvantage = bestMove.materialAdvantage;
                }
            }
        }
    }

    private double calculateStrategicAdvantage() {
        double whiteAttacks = futures.stream()
                .filter(future -> future.valid && future.white)
                .mapToDouble(future -> calculatePoints(future.endKey) + future.positionAdvantage).sum();
        double blackAttacks = futures.stream()
                .filter(future -> future.valid && !future.white)
                .mapToDouble(future -> calculatePoints(future.endKey) + future.positionAdvantage).sum();
        double kingQueenFactor = castleMove ? 3 : key == 'k' || key == 'K' || key == 'q' || key == 'Q' ? -3 : 0;

        return (white ? 1 : -1) * (kingQueenFactor) - (whiteAttacks + blackAttacks) / 100;
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
            ListIterator<Move> iterator = futures.listIterator();
            futures = new ArrayList<>();
            while (iterator.hasNext()) {
                Move future = iterator.next();
                Move mappedPosition = positionMap.get(future.position);
                if (mappedPosition == null) {
                    if (future.futures.isEmpty()) {
                        if (memoryUsage() < 0.9) {
                            future.generateFutures();
                        } else {
                            throw new OutOfMemoryError("Excessive memory usage");
                        }
                    }
                    if (future.valid && future.myMove) {
                        future.buildTree(branchDepth + 1, maxDepth, positionMap);
                        futures.add(future);
                        positionMap.put(future.position, future);
                    }
                } else {
                    futures.add(mappedPosition);
                }
            }
            if (futures.isEmpty()) {
                checkmate = true;
            }
        }
    }

    public Move findBestFuture(int maxDepth) {
        HashMap<String, Move> positionMap = new HashMap<>();
        if (futures.isEmpty()) {
            generateFutures();
        }
        if (maxDepth > 2) {
            for (int i=2; i<maxDepth; i++) {
                if (futures.size() == 1) {
                    return futures.stream().findFirst().get();
                }
                buildTree(0, i, positionMap);
                calculateAdvantage();
                pruneFutures(0, i, maxDepth);
                positionMap.clear();
            }
            if (futures.size() == 1) {
                return futures.stream().findFirst().get();
            }
            buildTree(0, maxDepth, positionMap);
        } else {
            buildTree(0, maxDepth, positionMap);
        }
        if (futures.isEmpty()) {
            return null;
        } else {
            calculateAdvantage();
            return findHighestAdvantage();
        }
    }

    public double memoryUsage() {
        long totalMemory = rt.totalMemory();
        long usedMemory = totalMemory - rt.freeMemory();
        return (double) usedMemory / totalMemory;
    }

    int sumFutures() {
        if (futures.isEmpty()) {
            return 0;
        } else {
            return futures.size() + futures.stream().map(Move::sumFutures).mapToInt(Integer::intValue).sum();
        }
    }

    private Move findHighestAdvantage() {
        if (futures.isEmpty()) {
            return null;
        } else {
            return white ? futures.stream().min(Comparator.comparing(Move::totalAdvantage)).get() :
                    futures.stream().max(Comparator.comparing(Move::totalAdvantage)).get();
        }
    }

    private void pruneFutures(int branchDepth, int pruneDepth, int maxDepth) {
        if (!futures.isEmpty()) {
            if (branchDepth < pruneDepth - 1) {
                futures.forEach(future -> future.pruneFutures(branchDepth + 1, pruneDepth, maxDepth));
            }
            Move bestFuture = findHighestAdvantage();
            if (bestFuture != null) {
                int i = 0;
                while (futures.size() > (maxDepth / pruneDepth) + 1 && i < 50) {
                    double range = 25 * Math.pow(0.9, i);
                    futures.removeIf(future -> white ? future.totalAdvantage() > bestFuture.totalAdvantage() + range :
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
