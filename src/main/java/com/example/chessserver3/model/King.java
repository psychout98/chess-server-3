package com.example.chessserver3.model;

import com.example.chessserver3.exception.InvalidMoveException;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
public class King extends Piece {

    private static final int[][] baseMoves = {{0, 1}, {0, -1}, {1, 1}, {1, 0}, {1, -1}, {-1, 1}, {-1, 0}, {-1, -1}};

    public King(int row, int col, boolean white, boolean shallow) {
        super(row, col, white, shallow);
    }

    @Override
    public void generateMoves(Board board) {
        addMoves(Set.of(baseMoves).stream().map(move -> new int[]{move[0] + getRow(), move[1] + getCol()})
                .filter(move -> {
                    try {
                        board.validateKingMove(isWhite(), move);
                        return true;
                    } catch (InvalidMoveException e) {
                        return false;
                    }
                }).collect(Collectors.toSet()), board);
    }

}
