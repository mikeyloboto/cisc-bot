package com.evilbas.discgm.util;

import com.evilbas.rslengine.character.Character;

public class CharacterUtils {
    public static Character createBlankCharacter(String name) {
        Character character = new Character(name);
        return character;
    }
}
