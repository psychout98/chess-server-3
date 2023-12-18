package com.example.chessserver3.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.HashMap;
import java.util.Set;

@Getter
public class Castle {

    public static final HashMap<String, String> castleRookMoveCode = new HashMap<>();
    public final static HashMap<String, String> castleMoveString = new HashMap<>();
    public final static HashMap<String, byte[]> castleRookMove = new HashMap<>();

    public static final HashMap<String, byte[][]> castleMoves = new HashMap<>();
    public static final HashMap<String, byte[][]> castleSpaces = new HashMap<>();
    public static final HashMap<String, byte[][]> castleRoutes = new HashMap<>();
    public final static HashMap<String, Boolean> initialValidCastle = new HashMap<>();
    public final static HashMap<String, String> castleKeys = new HashMap<>();
    static {
        castleRoutes.put("0402", new byte[][]{{0, 2}, {0, 3}, {0, 1}});
        castleRoutes.put("0406", new byte[][]{{0, 5}, {0, 6}});
        castleRoutes.put("7472", new byte[][]{{7, 2}, {7, 3}, {7, 1}});
        castleRoutes.put("7476", new byte[][]{{7, 5}, {7, 6}});
        castleSpaces.put("0402", new byte[][]{{0, 2}, {0, 3}, {0, 4}});
        castleSpaces.put("0406", new byte[][]{{0, 4}, {0, 5}, {0, 6}});
        castleSpaces.put("7472", new byte[][]{{7, 2}, {7, 3}, {7, 4}});
        castleSpaces.put("7476", new byte[][]{{7, 4}, {7, 5}, {7, 6}});
        castleMoves.put("0402", new byte[][]{{0, 2}, {0, 3}});
        castleMoves.put("0406", new byte[][]{{0, 5}, {0, 6}});
        castleMoves.put("7472", new byte[][]{{7, 2}, {7, 3}});
        castleMoves.put("7476", new byte[][]{{7, 5}, {7, 6}});
        castleRookMove.put("0402", new byte[]{0, 0, 0, 3});
        castleRookMove.put("0406", new byte[]{0, 7, 0, 5});
        castleRookMove.put("7472", new byte[]{7, 0, 7, 3});
        castleRookMove.put("7476", new byte[]{7, 7, 7, 5});
        castleRookMoveCode.put("0402", "0003");
        castleRookMoveCode.put("0406", "0705");
        castleRookMoveCode.put("7472", "7073");
        castleRookMoveCode.put("7476", "7775");
        castleMoveString.put("0402", "O-O-O");
        castleMoveString.put("0406", "O-O");
        castleMoveString.put("7472", "O-O-O");
        castleMoveString.put("7476", "O-O");
        initialValidCastle.put("0402", true);
        initialValidCastle.put("0406", true);
        initialValidCastle.put("7472", true);
        initialValidCastle.put("7476", true);
        castleKeys.put("0402", "q");
        castleKeys.put("0406", "k");
        castleKeys.put("7472", "Q");
        castleKeys.put("7476", "K");
    }
    private final static Set<String> castleMoveCodes = Set.of("0402", "0406", "7472", "7476");

}
