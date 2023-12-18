package com.example.chessserver3.model.computer;

import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.service.AdvantageComparator;
import lombok.*;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static com.example.chessserver3.model.board.Move.pointValues;

@Getter
public class AnalysisBoard extends RecursiveAction {

    private final ShortFEN shortFen;
    private boolean valid;
    private final byte depth;
    private final byte maxDepth;
    private final String lastMoveCode;
    private String bestMoveCode;
    private int advantage;

    public AnalysisBoard(ShortFEN shortFEN, byte depth, byte maxDepth, String lastMoveCode) {
        this.shortFen = shortFEN;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.lastMoveCode = lastMoveCode;
        this.bestMoveCode = "resign";
        this.advantage = calculateAdvantage(shortFEN.getBoardCode());
        this.valid = true;
    }

    private static int calculateAdvantage(String boardCode) {
        int materialAdvantage = 0;
        for (char key : boardCode.toCharArray()) {
            materialAdvantage += calculatePoints(key);
        }
        return materialAdvantage;
    }

    private static int calculatePoints(char key) {
        return Objects.requireNonNullElse(pointValues.get(key), 0);
    }

    @Override
    public void compute() {
        Set<ComputerMove> moves = new HashSet<>();
        char[][] boardKey = shortFen.getBoardKey();
        for (byte i=0;i<8;i++) {
            for (byte j=0;j<8;j++) {
                if (boardKey[i][j] != 'x') {
                    Boolean[][] moveMap = Board.moveMaps.get(boardKey[i][j]);
                    for (byte k = 0; k < moveMap.length; k++) {
                        for (byte l = 0; l < moveMap[k].length; l++) {
                            if (moveMap[k][l]) {
                                byte[] location = Board.pieceLocations.get(boardKey[i][j]);
                                byte row = (byte) (i + (k - location[0]));
                                byte col = (byte) (j + (l - location[1]));
                                if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                                    byte[] moveArray = {i, j, row, col};
                                    ComputerMove computerMove = new ComputerMove(moveArray, shortFen, depth, maxDepth);
                                    if (computerMove.isValid()) {
                                        moves.add(computerMove);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        valid = moves.stream()
                .noneMatch(computerMove -> Objects.equals(shortFen.getBoardKey()[computerMove.getEndRow()][computerMove.getEndCol()], (shortFen.isWhiteToMove() ? 'k' : 'K')));
        Collection<AnalysisBoard> futures = moves.stream().map(ComputerMove::getAnalysisBoard).collect(Collectors.toSet());
        if (depth < maxDepth) {
            ForkJoinTask.invokeAll(futures);
        }
        futures.removeIf(future -> !future.valid);
        AnalysisBoard bestFuture = shortFen.isWhiteToMove() ? Collections.max(futures, AdvantageComparator.advantageComparator) : Collections.min(futures, AdvantageComparator.advantageComparator);
        bestMoveCode = bestFuture.lastMoveCode;
        advantage = bestFuture.getAdvantage();
    }
}
