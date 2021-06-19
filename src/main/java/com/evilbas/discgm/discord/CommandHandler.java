package com.evilbas.discgm.discord;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;

import com.evilbas.discgm.discord.domain.MessageCommand;
import com.evilbas.discgm.service.UserService;
import com.evilbas.discgm.util.Constants;
import com.evilbas.rslengine.player.Player;
import com.evilbas.rslengine.player.PlayerState;

@Component
public class CommandHandler {

	private Map<String, MessageCommand> commands;

	@Autowired
	private UserService userService;

	@Autowired
	GatewayDiscordClient discordClient;

	@Value("${disc.self}")
	private Long selfId;

	@PostConstruct
	public void init() {

		discordClient.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
			final String content = event.getMessage().getContent();

			// Check if it is first interaction
			final Long playerId = event.getMessage().getAuthor().get().getId().asLong();

			if (playerId.equals(selfId)) {
				return;
			}

			Player player = userService.loadUser(playerId);

			for (final Map.Entry<String, MessageCommand> entry : getCommands().entrySet()) {
				if (content.startsWith(Constants.DISC_BOT_PREFIX + entry.getKey())) {
					entry.getValue().execute(event);
					break;
				}
			}
		});

		commands = new HashMap<String, MessageCommand>();
		commands.put("ping", event -> event.getMessage().getChannel().block().createMessage("Pong!").block());
		commands.put("start", event -> {

			event.getMessage().getChannel().block().createMessage("Pong!").block();
		});
		commands.put("status", event -> {
			if (event.getMessage().getChannel().block().getType().equals(Channel.Type.DM)) {
				Long playerId = event.getMessage().getAuthor().get().getId().asLong();
				event.getMessage().getChannel().block().createMessage(userService.getPlayerState(playerId).toString())
						.block();
				Message messageRef = event.getMessage().getChannel().block().createMessage("Checking").block();
				messageRef.pin().block();
			}
		});
		commands.put("me", event -> {
			// String sendingUser = event.getMessage().getAuthor().get().getUsername() + "#"
			// + event.getMessage().getAuthor().get().getDiscriminator();
			event.getMessage().getChannel().block()
					.createMessage("<@" + event.getMessage().getAuthor().get().getId().asLong() + ">").block();
		});
		commands.put("inventory", event -> {
			Long playerId = event.getMessage().getAuthor().get().getId().asLong();
			userService.updatePlayerState(playerId, PlayerState.INVENTORY);
			event.getMessage().getChannel().block().createMessage(userService.getPlayerState(playerId).toString())
					.block();
			Message messageRef = event.getMessage().getChannel().block().createMessage("Inventory").block();
			messageRef.pin().block();
		});

		commands.put("fight", event -> {
			Long playerId = event.getMessage().getAuthor().get().getId().asLong();
			userService.updatePlayerState(playerId, PlayerState.COMBAT);
			event.getMessage().getChannel().block().createMessage(userService.getPlayerState(playerId).toString())
					.block();
			Message messageRef = event.getMessage().getChannel().block().createMessage("Run Started").block();
			messageRef.pin().block();
		});

	}

	public Map<String, MessageCommand> getCommands() {
		return commands;
	}
}
