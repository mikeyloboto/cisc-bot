package com.evilbas.discgm.dao.sql;

import com.evilbas.rslengine.character.Character;
import com.evilbas.rslengine.player.Player;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CharacterMapper {

        @Select("SELECT character_guid FROM player_characters WHERE player_id = ${playerId} AND character_active = 'A'")
        public String getCurrentActiveCharacter(@Param("playerId") Integer playerId);

        @Insert("INSERT INTO player_characters (player_id, character_guid, character_active) VALUES (${player.playerId}, '${character.characterGuid}', 'A')")
        public void insertCharacter(@Param("player") Player player, @Param("character") Character character);
}
