package com.evilbas.discgm.service;

import com.evilbas.discgm.dao.sql.CharacterMapper;
import com.evilbas.discgm.dao.sql.UserMapper;
import com.evilbas.discgm.util.CharacterUtils;

import com.evilbas.rslengine.character.Character;
import com.evilbas.rslengine.player.Player;
import com.evilbas.rslengine.player.PlayerState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CharacterMapper characterMapper;

    @Autowired
    MongoTemplate mongoTemplate;

    public Player loadUser(Long playerId, String name) {
        Player player = userMapper.getPlayerById(playerId);

        if (player == null) {
            player = createNewPlayer(playerId, name);
        }

        log.info("User login ID: {}", player.getPlayerDiscId());

        return player;
    }

    @Transactional
    private Player createNewPlayer(Long playerId, String name) {
        log.info("Creating new user {}", playerId);
        Player newPlayer = new Player(playerId);
        userMapper.insertPlayer(newPlayer);
        Player player = userMapper.getPlayerById(playerId);
        userMapper.insertNewPlayerState(player.getPlayerId());
        Character character = CharacterUtils.createBlankCharacter(name);
        mongoTemplate.insert(character, "characters");
        characterMapper.insertCharacter(player, character);
        return player;
    }

    public PlayerState getPlayerState(Long playerId) {
        Player player = userMapper.getPlayerById(playerId);
        return userMapper.getPlayerState(player.getPlayerId());
    }

    public void updatePlayerState(Long playerId, PlayerState state) {
        Player player = userMapper.getPlayerById(playerId);
        userMapper.setPlayerState(player.getPlayerId(), state);
    }

    public Integer getPlayerId(Long playerId) {
        return userMapper.getPlayerById(playerId).getPlayerId();
    }
}
