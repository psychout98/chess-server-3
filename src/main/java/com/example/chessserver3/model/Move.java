package com.example.chessserver3.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Move {

    private String moveCode;
    private String moveString;
    private String boardKeyString;
    @BsonIgnore
    private int[] destination;
    @BsonIgnore
    private boolean attack;
}
