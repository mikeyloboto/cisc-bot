package com.evilbas.discgm.util;

import com.evilbas.rslengine.character.Character;
import com.evilbas.rslengine.player.PlayerState;
import com.evilbas.rslengine.util.CombatUtil;

public class CharacterUtils {
    public static Character createBlankCharacter(String name) {
        Character character = new Character(name);
        character.setEquippedWeapon(CombatUtil.generateStartingWeapon());
        character.setEquippedArmor(CombatUtil.generateStartingArmor());

        return character;
    }

    public static String stateToDesciption(PlayerState state) {
        switch (state) {
            case COMBAT:
                return "Fighting";
            case EXPLORE:
                return "Exploring world";
            case INVENTORY:
                return "Browsing inventory";
            case CRAFTING:
                return "Crafting";
            default:
                return "Slacking";
        }
    }
}
