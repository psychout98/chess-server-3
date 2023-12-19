package com.example.chessserver3.model.computer;

import com.example.chessserver3.model.board.Board;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class Piece {

    private char key;
    private byte row;
    private byte col;

//    public static List<byte[]> generateMoves(Piece p, BitBoard whitePieces, BitBoard blackPieces) {
//        List<byte[]> moves;
//        switch (p.key) {
//            case 'k', 'K' -> moves = kingMoves(p, whitePieces, blackPieces);
//        }
//    }
//
//    public static List<byte[]> kingMoves(Piece p, BitBoard whitePieces, BitBoard blackPieces) {
//        List<byte[]> moves = new ArrayList<>();
//
//    }
}
