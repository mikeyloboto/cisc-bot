package com.evilbas.discgm.discord;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;

import com.evilbas.discgm.discord.domain.MessageCommand;

@Component
public class CommandHandler {

	private Map<String, MessageCommand> commands;

	@PostConstruct
	public void init() {
		commands = new HashMap<String, MessageCommand>();
		commands.put("ping", event -> event.getMessage().getChannel().block().createMessage("Pong!").block());
		commands.put("status", event -> {
			if (event.getMessage().getChannel().block().getType().equals(Channel.Type.DM)) {
				Message messageRef = event.getMessage().getChannel().block().createMessage("Checking").block();
				messageRef.pin().block();
			}
		});
		commands.put("me", event -> {
			String sendingUser = event.getMessage().getAuthor().get().getUsername() + "#" + event.getMessage().getAuthor().get().getDiscriminator();
			event.getMessage().getChannel().block().createMessage("<@" + event.getMessage().getAuthor().get().getId().asLong() + ">").block();
		});

	}

	public Map<String, MessageCommand> getCommands() {
		return commands;
	}
}
