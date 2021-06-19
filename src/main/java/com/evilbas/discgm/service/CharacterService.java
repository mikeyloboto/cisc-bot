package com.evilbas.discgm.service;

import com.evilbas.rslengine.character.Character;
import com.evilbas.discgm.dao.sql.CharacterMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);

    @Autowired
    CharacterMapper characterMapper;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    UserService userService;

    public Character getActiveCharacterForPlayer(Integer playerId) {
        String guid = characterMapper.getCurrentActiveCharacter(playerId);
        log.info("guid: {}", guid);
        Query query = new Query(Criteria.where("characterGuid").is(guid));
        log.info("chars: {}", mongoTemplate.findOne(query, Character.class, "characters"));
        Character character = mongoTemplate.findOne(query, Character.class, "characters");

        // getCollection("characters").
        // .findOne(new Query(where("characterGuid").is(guid)), Character.class);
        log.info("character: {}", character);
        return character;
    }
}
