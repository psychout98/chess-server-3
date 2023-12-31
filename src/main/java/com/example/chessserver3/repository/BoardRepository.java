package com.example.chessserver3.repository;

import com.example.chessserver3.model.board.Board;
import com.mongodb.client.MongoCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.mongodb.client.model.Filters.*;

@Component
public class BoardRepository {

    @Autowired
    private MongoCollection<Board> boards;

    public void create(Board board) {
        boards.insertOne(board);
    }

    public Board findById(String boardId) {
        return boards.find(eq("_id", boardId)).first();
    }

    public void update(Board board) {
        boards.replaceOne(eq("_id", board.getId()), board);
    }

}
