package com.evilbas.discgm.service;

import com.evilbas.discgm.dao.sql.UserRepository;
import com.evilbas.rslengine.player.Player;
import com.evilbas.rslengine.player.PlayerState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    public Player loadUser(Long playerId) {
        Player player = userRepository.getPlayerById(playerId);

        if (player == null) {
            log.info("Creating new user {}", playerId);
            Player newPlayer = new Player(playerId);
            userRepository.insertPlayer(newPlayer);
            player = userRepository.getPlayerById(playerId);
            userRepository.insertNewPlayerState(player.getPlayerId());
        }

        log.info("User login ID: {}", player.getPlayerDiscId());

        return player;
    }

    public PlayerState getPlayerState(Long playerId) {
        Player player = userRepository.getPlayerById(playerId);
        return userRepository.getPlayerState(player.getPlayerId());
    }

    public void updatePlayerState(Long playerId, PlayerState state) {
        Player player = userRepository.getPlayerById(playerId);
        userRepository.setPlayerState(player.getPlayerId(), state);
    }

}
