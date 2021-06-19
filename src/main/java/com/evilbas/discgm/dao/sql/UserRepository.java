package com.evilbas.discgm.dao.sql;

import com.evilbas.rslengine.message.PlayerMessage;
import com.evilbas.rslengine.player.Player;
import com.evilbas.rslengine.player.PlayerState;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserRepository {
    @Select("SELECT * FROM players_ref WHERE player_disc_id = ${playerDiscId}")
    @Results(value = { @Result(property = "playerId", column = "player_id"),
            @Result(property = "playerDiscId", column = "player_disc_id") })
    public Player getPlayerById(@Param("playerDiscId") Long playerDiscId);

    @Insert("INSERT INTO players_ref (player_disc_id) VALUES (${player.playerDiscId})")
    public Integer insertPlayer(@Param("player") Player player);

    @Select("SELECT * FROM player_key_message where player_id = ${playerId}")
    @Results(value = { @Result(property = "playerId", column = "player_id"),
            @Result(property = "messageId", column = "message_id"),
            @Result(property = "messageType", column = "message_type") })
    public PlayerMessage getMessagesForPlayer(@Param("playerId") Integer playerId);

    @Insert("INSERT INTO player_key_message (player_id, message_id, message_type) values (${message.playerId}, ${message.messageId}, '${message.messageType}')")
    public void insertKeyMessage(@Param("message") PlayerMessage message);

    @Delete("DELETE FROM player_key_message WHERE message_id = ${messageId}")
    public void deleteKeyMessage(@Param("messageId") String messageId);

    @Select("SELECT player_state FROM players_state WHERE player_id = ${playerId}")
    public PlayerState getPlayerState(@Param("playerId") Integer playerId);

    @Insert("INSERT INTO players_state (player_id) VALUES (${playerId})")
    public void insertNewPlayerState(@Param("playerId") Integer playerId);

    @Update("UPDATE players_state SET player_state = '${playerState}' WHERE player_id = ${playerId}")
    public void setPlayerState(@Param("playerId") Integer playerId, @Param("playerState") PlayerState string);
}
