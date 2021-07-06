package com.evilbas.discgm.dao.sql;

import com.evilbas.discgm.domain.KeyMessage;
import com.evilbas.discgm.domain.KeyMessageType;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KeyMessageMapper {
    @Select("SELECT * FROM player_key_message WHERE player_id = ${playerId}")
    @Results(value = { @Result(property = "playerId", column = "player_id"),
            @Result(property = "messageId", column = "message_id"),
            @Result(property = "messageType", column = "message_type") })
    public KeyMessage findMessageByPlayerId(@Param("playerId") Integer playerId);

    @Insert("INSERT INTO player_key_message (player_id, message_id, message_type) VALUES (${message.playerId}, '${message.messageId}', '${message.messageType}')")
    public void saveKeyMessage(@Param("message") KeyMessage message);

    @Select("SELECT message_type FROM player_key_message WHERE message_id = ${messageId}")
    public KeyMessageType getMessageType(@Param("messageId") String messageId);
}
