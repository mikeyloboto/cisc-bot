package com.evilbas.discgm.dao.mongo;

import com.evilbas.rslengine.player.Player;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoUserRepository extends MongoRepository<Player, String> {
}
