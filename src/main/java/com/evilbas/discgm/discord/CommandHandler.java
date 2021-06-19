package com.evilbas.discgm.discord;

import java.time.Instant;
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
import discord4j.rest.util.Color;

import com.evilbas.discgm.discord.domain.MessageCommand;
import com.evilbas.discgm.service.CharacterService;
import com.evilbas.discgm.service.UserService;
import com.evilbas.discgm.util.Constants;
import com.evilbas.rslengine.character.Character;
import com.evilbas.rslengine.player.Player;
import com.evilbas.rslengine.player.PlayerState;

@Component
public class CommandHandler {

	private Map<String, MessageCommand> commands;

	@Autowired
	private UserService userService;

	@Autowired
	private CharacterService characterService;

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

			Player player = userService.loadUser(playerId,
					"<@" + event.getMessage().getAuthor().get().getId().asLong() + ">");

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
			// if
			// (event.getMessage().getChannel().block().getType().equals(Channel.Type.DM)) {
			Long playerDiscId = event.getMessage().getAuthor().get().getId().asLong();
			Integer playerId = userService.getPlayerId(playerDiscId);
			Character character = characterService.getActiveCharacterForPlayer(playerId);

			Message statusMessageRef = event.getMessage().getChannel().block()
					.createEmbed(spec -> spec.setColor(Color.BLUE).setAuthor("CISC Bot", "", "")
							.setTitle(character.getCharacterName())
							.setDescription("Level: " + character.getCharacterLevel() + "\n" + "Current Exp: "
									+ character.getCharacterExp())
							.addField("Level", character.getCharacterLevel().toString(), true)
							.addField("Exp", character.getCharacterExp().toString(), true)
							.addField("Class", "Warlock", true).setTimestamp(Instant.now()))
					.block();
			// Message messageRef =
			// event.getMessage().getChannel().block().createMessage("Checking").block();
			statusMessageRef.pin().block();
			// }
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
