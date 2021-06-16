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
    @Select("SELECT * FROM players_ref WHERE player_disc_id = ${playerDiscId}")
    @Results(value = { @Result(property = "playerId", column = "player_id"),
            @Result(property = "playerDiscId", column = "player_disc_id") })
    public Player getPlayerById(@Param("playerDiscId") Long playerDiscId);

    @Insert("INSERT INTO players_ref (player_disc_id) VALUES (${player.playerDiscId})")
    public Player insertPlayer(@Param("player") Player player);
}
