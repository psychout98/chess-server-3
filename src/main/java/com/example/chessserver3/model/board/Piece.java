package com.example.chessserver3.model.board;

import com.example.chessserver3.exception.InvalidMoveException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public abstract class Piece {

    private int row;
    private int col;
    private boolean white;
    private Set<Move> moves;
    private boolean shallow;

    public Piece(int row, int col, boolean white, boolean shallow) {
        this.row = row;
        this.col = col;
        this.white = white;
        this.moves = new HashSet<>();
        this.shallow = shallow;
    }

    public void move(int[] move) {
        if (moves.stream().anyMatch(m -> Arrays.equals(m.getDestination(), move))) {
            setRow(move[0]);
            setCol(move[1]);
        } else {
            throw new InvalidMoveException(String.format("Invalid move %s%s%s%s", row, col, move[0], move[1]));
        }
    }

    public abstract void generateMoves(Board board);

    public abstract int getPoints();


    public void addMove(Board board, int[] move) {
        addMove(board, move, true);
    }

    public void addMove(Board board, int[] move, boolean attack) {
        addMove(board, new Move(String.format("%s%s%s%s", row, col, move[0], move[1]), null, null, move, attack));
    }
    
    public void addMove(Board board, Move move) {
        if (isValidMove(move, board)) {
            moves.add(move);
        }
    }

    public void addMoves(Board board, Set<int[]> moves) {
        moves.forEach(move -> addMove(board, move));
    }

    public boolean isObstructed(int[] move, String[][] boardKey, boolean isWhite) {
        return boardKey[move[0]][move[1]].startsWith(isWhite ? "w" : "b");
    }

    public boolean isOnBoard(int[] move) {
        return move[0] < 8 && move[0] >= 0 && move[1] < 8 && move[1] >= 0;
    }

    public boolean isValidMove(Move move, Board board) {
        if (isOnBoard(move.getDestination()) && !isObstructed(move.getDestination(), board.getBoardKey(), white)) {
            if (!shallow) {
                Board nextBoard = board.shallowCopy(board.getHistory().size() - 1);
                try {
                    nextBoard.move(move.getMoveCode(), white);
                    return true;
                } catch (InvalidMoveException e) {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public void addRookMoves(Board board) {
        for (int i=col + 1; i<8; i++) {
            int[] move = {row, i};
            if (isObstructed(move, board.getBoardKey(), white)) {
                break;
            }
            addMove(board, move);
            if (isObstructed(move, board.getBoardKey(), !white)) {
                break;
            }
        }
        for (int i=col - 1; i>=0; i--) {
            int[] move = {row, i};
            if (isObstructed(move, board.getBoardKey(), white)) {
                break;
            }
            addMove(board, move);
            if (isObstructed(move, board.getBoardKey(), !white)) {
                break;
            }
        }
        for (int i=row + 1; i<8; i++) {
            int[] move = {i, col};
            if (isObstructed(move, board.getBoardKey(), white)) {
                break;
            }
            addMove(board, move);
            if (isObstructed(move, board.getBoardKey(), !white)) {
                break;
            }
        }
        for (int i=row - 1; i>=0; i--) {
            int[] move = {i, col};
            if (isObstructed(move, board.getBoardKey(), white)) {
                break;
            }
            addMove(board, move);
            if (isObstructed(move, board.getBoardKey(), !white)) {
                break;
            }
        }
    }

    public void addBishopMoves(Board board) {
        for (int i=row + 1, j=col + 1; i<8 && j<8; i++, j++) {
            int[] move = {i, j};
            if (isObstructed(move, board.getBoardKey(), white)) {
                break;
            }
            addMove(board, move);
            if (isObstructed(move, board.getBoardKey(), !white)) {
                break;
            }
        }
        for (int i=row - 1, j=col + 1; i>=0 && j<8; i--, j++) {
            int[] move = {i, j};
            if (isObstructed(move, board.getBoardKey(), white)) {
                break;
            }
            addMove(board, move);
            if (isObstructed(move, board.getBoardKey(), !white)) {
                break;
            }
        }
        for (int i=row - 1, j=col - 1; i>=0 && j>=0; i--, j--) {
            int[] move = {i, j};
            if (isObstructed(move, board.getBoardKey(), white)) {
                break;
            }
            addMove(board, move);
            if (isObstructed(move, board.getBoardKey(), !white)) {
                break;
            }
        }
        for (int i=row + 1, j=col - 1; i<8 && j>=0; i++, j--) {
            int[] move = {i, j};
            if (isObstructed(move, board.getBoardKey(), white)) {
                break;
            }
            addMove(board, move);
            if (isObstructed(move, board.getBoardKey(), !white)) {
                break;
            }
        }
    }
}
