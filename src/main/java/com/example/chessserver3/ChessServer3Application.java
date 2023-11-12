package com.example.chessserver3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

@SpringBootApplication
@EnableMongoRepositories
public class ChessServer3Application {

    public static void main(String[] args) {
        SpringApplication.run(ChessServer3Application.class, args);
    }

}
