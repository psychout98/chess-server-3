package com.example.chessserver3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChessServer3Application {

    public static void main(String[] args) {
        SpringApplication.run(ChessServer3Application.class, args);
    }

}
