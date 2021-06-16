package com.evilbas.discgm.service;

import java.util.List;

import com.evilbas.discgm.dao.sql.UserRepository;
import com.evilbas.rslengine.player.Player;

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
        log.debug(player.toString());

        if (player == null) {
            Player newPlayer = new Player(playerId);
            userRepository.insertPlayer(player);
            player = userRepository.getPlayerById(playerId);
        }

        return player;
    }

}
