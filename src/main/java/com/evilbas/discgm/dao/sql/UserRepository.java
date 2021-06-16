package com.evilbas.discgm.dao.sql;

import com.evilbas.rslengine.player.Player;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRepository {
    @Select("SELECT * FROM players_ref WHERE player_id = ${playerId}")
    @Results(value = { @Result(property = "playerId", column = "player_id") })
    public Player getPlayerById(@Param("playerId") Long playerId);

    @Insert("INSERT INTO players_ref (player_id) VALUES ($(player.playerId))")
    public Player insertPlayer(@Param("player") Player player);
}
