package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import com.example.chessserver3.exception.KingCheckedException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.Arrays;
import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Piece {

    private int row;
    private int col;
    private boolean white;
    private Set<Move> moves;
    private boolean shallow;
    @JsonIgnore
    private Board board;
    private boolean kingAttacker;

    public void move(int[] move) {
        if (moves.stream().anyMatch(m -> Arrays.equals(m.getDestination(), move))) {
            row = move[0];
            col = move[1];
        } else {
            throw new InvalidMoveException(String.format("Invalid move %s%s%s%s", row, col, move[0], move[1]));
        }
    }

    public abstract void generateMoves();

    public abstract int getPoints();


    public void addMove(int[] move) {
        addMove(move, true);
    }

    public void addMove(int[] move, boolean attack) {
        addMove(new Move(String.format("%s%s%s%s", row, col, move[0], move[1]), null, null, move, attack));
    }
    
    public void addMove(Move move) {
        if (isValidMove(move)) {
            moves.add(move);
        }
    }

    public void addMoves(Set<int[]> moves) {
        moves.forEach(this::addMove);
    }

    public boolean isObstructed(int[] move, boolean isWhite) {
        return board.getBoardKey()[move[0]][move[1]].startsWith(isWhite ? "w" : "b");
    }

    public boolean isOnBoard(int[] move) {
        return move[0] < 8 && move[0] >= 0 && move[1] < 8 && move[1] >= 0;
    }

    public boolean isValidMove(Move move) {
        if (isOnBoard(move.getDestination()) && !isObstructed(move.getDestination(), white)) {
            if (!shallow) {
                Board nextBoard = board.copy(true);
                try {
                    nextBoard.move(move.getMoveCode(), white);
                    return true;
                } catch (InvalidMoveException e) {
                    return false;
                } catch (KingCheckedException e) {
                    kingAttacker = true;
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
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
