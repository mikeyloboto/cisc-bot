package com.evilbas.discgm.discord.domain;

import discord4j.core.event.domain.message.MessageCreateEvent;

public interface MessageCommand {
    void execute(MessageCreateEvent event);

}
