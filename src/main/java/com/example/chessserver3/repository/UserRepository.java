package com.example.chessserver3.repository;

import com.example.chessserver3.model.user.User;
import com.mongodb.client.MongoCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static com.mongodb.client.model.Filters.*;

@Component
public class UserRepository {

    @Autowired
    private MongoCollection<User> users;

    public void create(User user) { users.insertOne(user); }

    public User findByUsername(String username) {
        return users.find(eq("username", username)).first();
    }

    public User findByPlayerId(String playerId) {
        return users.find(eq("playerId", playerId)).first();
    }

    public void update(User user) { users.replaceOne(eq("username", user.getUsername()), user); }
}
