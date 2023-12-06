package com.example.chessserver3.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.HashMap;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Castle {

    private HashMap<String, Boolean> validCastles;
    @BsonIgnore
    @JsonIgnore
    public static final HashMap<String, String> castleRookMoveCode = new HashMap<>();
    @BsonIgnore
    @JsonIgnore
    public final static HashMap<String, String> castleMoveString = new HashMap<>();
    @BsonIgnore
    @JsonIgnore
    public final static HashMap<String, int[]> castleRookMove = new HashMap<>();
    @BsonIgnore
    @JsonIgnore
    public static final HashMap<String, int[][]> castleMoves = new HashMap<>();
    @BsonIgnore
    @JsonIgnore
    public static final HashMap<String, int[][]> castleSpaces = new HashMap<>();
    @BsonIgnore
    @JsonIgnore
    public final static HashMap<String, Boolean> initialValidCastle = new HashMap<>();
    static {
        castleSpaces.put("0402", new int[][]{{0, 2}, {0, 3}, {0, 4}});
        castleSpaces.put("0406", new int[][]{{0, 4}, {0, 5}, {0, 6}});
        castleSpaces.put("7472", new int[][]{{7, 2}, {7, 3}, {7, 4}});
        castleSpaces.put("7476", new int[][]{{7, 4}, {7, 5}, {7, 6}});
        castleMoves.put("0402", new int[][]{{0, 2}, {0, 3}});
        castleMoves.put("0406", new int[][]{{0, 5}, {0, 6}});
        castleMoves.put("7472", new int[][]{{7, 2}, {7, 3}});
        castleMoves.put("7476", new int[][]{{7, 5}, {7, 6}});
        castleRookMove.put("0402", new int[]{0, 0, 0, 3});
        castleRookMove.put("0406", new int[]{0, 7, 0, 5});
        castleRookMove.put("7472", new int[]{7, 0, 7, 3});
        castleRookMove.put("7476", new int[]{7, 7, 7, 5});
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
    }
    @BsonIgnore
    @JsonIgnore
    private final static Set<String> castleMoveCodes = Set.of("0402", "0406", "7472", "7476");

    public Castle copy() {
        return Castle.builder().validCastles(new HashMap<>(validCastles)).build();
    }

    public static Castle initialCastle() {
        return Castle.builder().validCastles(initialValidCastle).build();
    }

    public static boolean isCastle(String moveCode) {
        return castleMoveCodes.contains(moveCode);
    }

    public void checkCastles(String key) {
        switch (key) {
            case "wk" -> {
                validCastles.put("0402", false);
                validCastles.put("0406", false);
            }
            case "bk" -> {
                validCastles.put("7472", false);
                validCastles.put("7476", false);
            }
            case "wr1" -> validCastles.put("0402", false);
            case "wr2" -> validCastles.put("0406", false);
            case "br1" -> validCastles.put("7472", false);
            case "br2" -> validCastles.put("7476", false);
        }
    }
}
