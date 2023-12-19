package com.example.chessserver3.model.computer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MoveNode {

    private final byte[] moveArray;
    private final MoveNode next;


}
