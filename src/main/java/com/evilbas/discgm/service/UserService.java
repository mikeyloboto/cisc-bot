package com.evilbas.discgm.service;

import java.util.List;

import com.evilbas.discgm.dao.mongo.UserRepository;
import com.evilbas.rslengine.player.Player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Player loadUser(Long playerId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("playerId").is(playerId));
        List<Player> player = mongoTemplate.find(query, Player.class);
        log.debug("player");
        return null;
    }

}
