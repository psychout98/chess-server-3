package com.example.chessserver3.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Piece {

    private int row;
    private int col;
    private boolean white;
    private Set<String> moves;
    @JsonIgnore
    private Board board;

    public abstract void generateMoves();

    public void addMove(String move) {
        moves.add(move);
    }

    public void addMove(int[] move) {
        addMove(String.format("%s%s%s%s", row, col, move[0], move[1]));
    }

    public void addMoves(Set<int[]> moves) {
        moves.forEach(this::addMove);
    }

    public boolean isObstructed(int[] move) {
        return !board.getBoardKey()[move[0]][move[1]].isEmpty();
    }

    public boolean isObstructed(int[] move, boolean isWhite) {
        return board.getBoardKey()[move[0]][move[1]].startsWith(isWhite ? "w" : "b");
    }

    public boolean isOnBoard(int[] move) {
        return move[0] < 8 && move[0] >= 0 && move[1] < 8 && move[1] >= 0;
    }

    public void addRookMoves() {
        for (int i=col + 1; i<8; i++) {
            int[] move = {row, i};
            if (isObstructed(move, white)) {
                break;
            }
            addMove(move);
            if (isObstructed(move, !white)) {
                break;
            }
        }
        for (int i=col - 1; i>=0; i--) {
            int[] move = {row, i};
            if (isObstructed(move, white)) {
                break;
            }
            addMove(move);
            if (isObstructed(move, !white)) {
                break;
            }
        }
        for (int i=row + 1; i<8; i++) {
            int[] move = {i, col};
            if (isObstructed(move, white)) {
                break;
            }
            addMove(move);
            if (isObstructed(move, !white)) {
                break;
            }
        }
        for (int i=row - 1; i>=0; i--) {
            int[] move = {i, col};
            if (isObstructed(move, white)) {
                break;
            }
            addMove(move);
            if (isObstructed(move, !white)) {
                break;
            }
        }
    }

    public void addBishopMoves() {
        for (int i=row + 1, j=col + 1; i<8 && j<8; i++, j++) {
            int[] move = {i, j};
            if (isObstructed(move, white)) {
                break;
            }
            addMove(move);
            if (isObstructed(move, !white)) {
                break;
            }
        }
        for (int i=row - 1, j=col + 1; i>=0 && j<8; i--, j++) {
            int[] move = {i, j};
            if (isObstructed(move, white)) {
                break;
            }
            addMove(move);
            if (isObstructed(move, !white)) {
                break;
            }
        }
        for (int i=row - 1, j=col - 1; i>=0 && j>=0; i--, j--) {
            int[] move = {i, j};
            if (isObstructed(move, white)) {
                break;
            }
            addMove(move);
            if (isObstructed(move, !white)) {
                break;
            }
        }
        for (int i=row + 1, j=col - 1; i<8 && j>=0; i++, j--) {
            int[] move = {i, j};
            if (isObstructed(move, white)) {
                break;
            }
            addMove(move);
            if (isObstructed(move, !white)) {
                break;
            }
        }
    }
}
