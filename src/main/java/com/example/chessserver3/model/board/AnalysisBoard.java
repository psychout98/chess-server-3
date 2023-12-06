package com.example.chessserver3.model.board;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class AnalysisBoard extends RecursiveTask<Move> {

    private final String oldBoardKeyString;
    private final Move previousMove;
    @Builder.Default
    private final Collection<AnalysisBoard> branches = new ArrayList<>();
    private Move bestMove;
    private static final Random random = new Random();
    private Integer highestAdvantage;
    private List<Move> validFutures;
    private boolean root;
    @Builder.Default
    private HashMap<String, Move> mappedMoves = new HashMap<>();
    @Builder.Default
    private HashMap<String, AnalysisBoard> mappedPositions = new HashMap<>();
    private int depth;

    private static boolean advantageous(final int currentAdvantage, final int newAdvantage, final boolean whiteToMove) {
        if (whiteToMove) {
            return newAdvantage > currentAdvantage;
        } else {
            return newAdvantage < currentAdvantage;
        }
    }

    private static boolean justAsGood(final int currentAdvantage, final int newAdvantage, final boolean whiteToMove) {
        if (whiteToMove) {
            return newAdvantage >= currentAdvantage;
        } else {
            return newAdvantage <= currentAdvantage;
        }
    }

    private void growBranches(int branchDepth) {
        if (branches.isEmpty()) {
            for (Move move : validFutures) {
                if (mappedPositions.get(move.getBoardKeyString()) == null) {
                    AnalysisBoard branch = AnalysisBoard.builder()
                            .depth(branchDepth)
                            .oldBoardKeyString(previousMove.getBoardKeyString())
                            .previousMove(move)
                            .mappedMoves(mappedMoves)
                            .mappedPositions(mappedPositions)
                            .root(false)
                            .build();
                    branch.generateFutures();
                    if (branch.previousMove.isValid()) {
                        branches.add(branch);
                    }
                }
            }
        } else {
            resetBranches();
        }
    }

    private void resetBranches() {
        branches.forEach(branch -> {
            branch.bestMove = null;
            branch.highestAdvantage = null;
        });
    }

    private void pruneBranches() {
        System.out.println(bestMove.getMoveString() + " " + highestAdvantage);
        System.out.println(branches.stream().map(branch -> branch.previousMove.getMoveString() + " " + branch.highestAdvantage).toList());
        int before = branches.size();
        branches.removeIf(branch -> !justAsGood(highestAdvantage, branch.highestAdvantage, !previousMove.isWhite()));
        System.out.println("pruned: " + (before - branches.size()) + " of " + before + " branches");
    }

    private void pickRandomBranch() {
        if (!branches.isEmpty()) {
            while (bestMove == null) {
                bestMove = branches.stream().toList().get(random.nextInt(branches.size())).getPreviousMove();
                highestAdvantage = bestMove.getAdvantage();
            }
        }
    }

    private void findBestBranch() {
        if (bestMove != null) {
            for (AnalysisBoard branch : branches) {
                if (branch.bestMove != null && advantageous(highestAdvantage, branch.highestAdvantage, !previousMove.isWhite())) {
                        bestMove = branch.getPreviousMove();
                        highestAdvantage = branch.highestAdvantage;
                }
            }
        }
    }

    public void generateFutures() {
        previousMove.generateFutures(oldBoardKeyString, previousMove.isWhite());
        previousMove.getFutures().forEach(move -> move.generateFutures(previousMove.getBoardKeyString(), !previousMove.isWhite()));
        validFutures = previousMove.getFutures().stream().filter(Move::isValid).collect(Collectors.toList());
    }

    private void searchBranches() {
        pickRandomBranch();
        findBestBranch();
    }

    private void findBestMove() {
        Move mapped = mappedMoves.get(previousMove.getBoardKeyString());
        if (mapped == null) {
            bestMove = validFutures.get(random.nextInt(validFutures.size()));
            highestAdvantage = bestMove.getAdvantage();
            for (Move move : validFutures) {
                if (advantageous(highestAdvantage, move.getAdvantage(), previousMove.isWhite())) {
                    bestMove = move;
                    highestAdvantage = move.getAdvantage();
                }
            }
            mappedMoves.put(previousMove.getBoardKeyString(), bestMove);
        } else {
            bestMove = mapped;
            highestAdvantage = mapped.getAdvantage();
        }
    }

    private void computeBranches(int branchDepth) {
        branches.forEach(branch -> branch.depth = branchDepth);
        ForkJoinTask.invokeAll(branches).forEach(ForkJoinTask::join);
    }

    @Override
    public Move compute() {
        if (depth > 0) {
            System.out.print(depth);
            if (root && depth > 2) {
                for (int i = 2; i <= depth; i++) {
                    System.out.println("root at depth " + i);
                    if (i > 2) {
                        System.out.println("pruning");
                        pruneBranches();
                    }
                    System.out.println("growing");
                    growBranches(i);
                    System.out.println("computing");
                    computeBranches(i);
                    System.out.println("searching");
                    searchBranches();
                }
            } else {
                growBranches(depth - 1);
                computeBranches(depth - 1);
                searchBranches();
            }
        } else {
            findBestMove();
        }
        mappedPositions.put(previousMove.getBoardKeyString(), this);
        return bestMove;
    }
}