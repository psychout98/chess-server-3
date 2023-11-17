package com.example.chessserver3.service;

import com.example.chessserver3.exception.IncorrectPasswordException;
import com.example.chessserver3.exception.UserNotFoundException;
import com.example.chessserver3.exception.UsernameTakenException;
import com.example.chessserver3.model.user.User;
import com.example.chessserver3.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void createUser(String username, String password, String playerId) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            userRepository.create(new User(username, password, playerId, new ArrayList<>()));
        } else {
            throw new UsernameTakenException("Username taken");
        }
    }

    public String getPlayerIdForUser(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            if (Objects.equals(user.getPassword(), password)) {
                return user.getPlayerId();
            } else {
                throw new IncorrectPasswordException("Incorrect password");
            }
        } else {
            throw new UserNotFoundException("Invalid username");
        }
    }

    public void addGameToUser(String playerId, String boardId) {
        User user = userRepository.findByPlayerId(playerId);
        if (user != null) {
            user.addGame(boardId);
            userRepository.update(user);
        }
    }

    public List<String> getBoardIdsByPlayerName(String playerName) {
        return userRepository.findByUsername(playerName).getGames();
    }
}
