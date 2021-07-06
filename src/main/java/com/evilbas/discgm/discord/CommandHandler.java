package com.evilbas.discgm.discord;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import com.evilbas.discgm.client.ProcServerClient;
import com.evilbas.discgm.dao.sql.KeyMessageMapper;
import com.evilbas.discgm.discord.domain.MessageCommand;
import com.evilbas.discgm.domain.KeyMessage;
import com.evilbas.discgm.domain.KeyMessageType;
import com.evilbas.discgm.service.CharacterService;
import com.evilbas.discgm.service.UserService;
import com.evilbas.discgm.util.CharacterUtils;
import com.evilbas.discgm.util.Constants;
import com.evilbas.rslengine.character.Character;
import com.evilbas.rslengine.creature.Creature;
import com.evilbas.rslengine.creature.Encounter;
import com.evilbas.rslengine.player.Player;
import com.evilbas.rslengine.player.PlayerState;

@Component
public class CommandHandler {

	private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

	private Map<String, MessageCommand> commands;

	@Autowired
	private UserService userService;

	@Autowired
	private CharacterService characterService;

	@Autowired
	private KeyMessageMapper keyMessageMapper;

	@Autowired
	GatewayDiscordClient discordClient;

	@Autowired
	private ProcServerClient procServerClient;

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

			for (final Map.Entry<String, MessageCommand> entry : getCommands().entrySet()) {
				if (content.startsWith(Constants.DISC_BOT_PREFIX + entry.getKey())) {
					entry.getValue().execute(event);
					break;
				}
			}
		});

		discordClient.getEventDispatcher().on(ReactionAddEvent.class).subscribe(event -> {
			final Long playerId = event.getUser().block().getId().asLong();

			if (playerId.equals(selfId)) {
				return;
			}
			String messageId = event.getMessage().block().getId().asString();
			log.info("messageId: {}", messageId);
			log.info("messageType: {}", keyMessageMapper.getMessageType(messageId));
			log.info("emoji: {}", event.getEmoji().asUnicodeEmoji().get().getRaw());
			switch (keyMessageMapper.getMessageType(messageId)) {
				case STATUS:
					switch (event.getEmoji().asUnicodeEmoji().get().getRaw()) {
						case "⚔️":
							combatCommand(playerId, event.getMessage().block().getChannel().block());
							break;
						case "🎒":
							inventoryCommand(playerId, event.getMessage().block().getChannel().block());
							break;
						case "🛠️":
							craftingCommand(playerId, event.getMessage().block().getChannel().block());
							break;
					}
					break;
				case COMBAT:
					if (event.getEmoji().asUnicodeEmoji().get().getRaw().equals("⚔️")) {
						// autoattack case
						attackCommand(playerId, event.getMessage().block().getChannel().block(), null,
								event.getMessage().block());
					}
					break;
			}

		});

		commands = new HashMap<>();
		commands.put("ping", event -> event.getMessage().getChannel().block().createMessage("Pong!").block());
		commands.put("start", event -> {

			event.getMessage().getChannel().block().createMessage("Pong!").block();
		});
		commands.put("status", event -> {
			if (event.getMessage().getChannel().block().getType().equals(Channel.Type.DM)) {
				Long playerDiscId = event.getMessage().getAuthor().get().getId().asLong();
				Integer playerId = userService.getPlayerId(playerDiscId);
				Character character = characterService.getActiveCharacterForPlayer(playerId);
				Message statusMessageRef = event.getMessage().getChannel().block().createEmbed(spec -> spec
						.setColor(Color.BLUE).setAuthor("CISC Bot", "", "").setTitle("Status")
						.setDescription("Level: " + character.getCharacterLevel() + "\n" + "Current Exp: "
								+ character.getCharacterExp())
						.addField("Level", character.getCharacterLevel().toString(), true)
						.addField("Exp", character.getCharacterExp().toString(), true)
						.addField("Class", "Warrior", true).setTimestamp(Instant.now()).addField("Currently",
								CharacterUtils.stateToDesciption(userService.getPlayerState(playerDiscId))
										+ ((userService.getPlayerState(playerDiscId) == PlayerState.COMBAT)
												? " " + character.getCurrentEncounter().getCreatureSlot(0).getName()
												: ""),
								true))
						.block();
				statusMessageRef.addReaction(ReactionEmoji.unicode("⚔️")).block();
				statusMessageRef.addReaction(ReactionEmoji.unicode("🎒")).block();
				statusMessageRef.addReaction(ReactionEmoji.unicode("🗺️")).block();
				statusMessageRef.addReaction(ReactionEmoji.unicode("💰")).block();
				statusMessageRef.addReaction(ReactionEmoji.unicode("🛠️")).block();
				keyMessageMapper.saveKeyMessage(
						new KeyMessage(playerId, statusMessageRef.getId().asString(), KeyMessageType.STATUS));
				log.info("Message: {}", statusMessageRef.getId().asString());
			}
		});

		commands.put("me", event -> {
			event.getMessage().getChannel().block()
					.createMessage("<@" + event.getMessage().getAuthor().get().getId().asLong() + ">").block();
		});

		commands.put("inventory", event -> {
			Long playerId = event.getMessage().getAuthor().get().getId().asLong();
			inventoryCommand(playerId, event.getMessage().getChannel().block());

		});

		commands.put("fight", event -> {

			Long playerId = event.getMessage().getAuthor().get().getId().asLong();
			combatCommand(playerId, event.getMessage().getChannel().block());
		});

		commands.put("craft", event -> {

			Long playerId = event.getMessage().getAuthor().get().getId().asLong();
			craftingCommand(playerId, event.getMessage().getChannel().block());
		});

	}

	public Map<String, MessageCommand> getCommands() {
		return commands;
	}

	private void combatCommand(Long playerDiscId, MessageChannel channel) {
		Integer playerId = userService.getPlayerId(playerDiscId);
		userService.updatePlayerState(playerDiscId, PlayerState.COMBAT);
		Character character = characterService.getActiveCharacterForPlayer(playerId);
		Encounter encounter = procServerClient
				.retrieveEncounter(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid());

		// TEMP
		// channel.createMessage(userService.getPlayerState(playerDiscId).toString()).block();
		// Message messageRef = channel.createMessage("Run Started " +
		// encounter.getCreatureSlot(0)).block();

		Mono<Message> combatMessageRefBuild = channel.createEmbed(spec -> {
			spec.setColor(Color.BLUE).setAuthor("CISC Bot", "", "").setTitle("Combat")
					.setDescription("Enemies: " + encounter.getCreatures().size() + "\n" + "Encounter Exp: "
							+ encounter.getEncounterExp(character))
					// .addField("Level", character.getCharacterLevel().toString(), true)
					// .addField("Exp", character.getCharacterExp().toString(),
					// true).addField("Class", "Warrior", true)
					.setTimestamp(Instant.now());
			for (Creature c : encounter.getCreatures()) {
				spec.addField("Lvl " + c.getLevel() + " " + c.getName(), c.getCurrentHp() + "/" + c.getMaxHp(), false);
			}
		});

		Message combatMessageRef = combatMessageRefBuild.block();
		combatMessageRef.addReaction(ReactionEmoji.unicode("⚔️")).block();

		keyMessageMapper
				.saveKeyMessage(new KeyMessage(playerId, combatMessageRef.getId().asString(), KeyMessageType.COMBAT));
	}

	private void attackCommand(Long playerDiscId, MessageChannel channel, String attack, Message oldMessage) {
		oldMessage.delete().block();
		Integer playerId = userService.getPlayerId(playerDiscId);

		// TODO: adjust spell slots
		procServerClient.performAttack(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid(), 0, 0);

		combatCommand(playerDiscId, channel);

	}

	private void inventoryCommand(Long playerDiscId, MessageChannel channel) {

		Integer playerId = userService.getPlayerId(playerDiscId);

		userService.updatePlayerState(playerDiscId, PlayerState.INVENTORY);
		channel.createMessage(userService.getPlayerState(playerDiscId).toString()).block();
		Message messageRef = channel.createMessage("Inventory").block();
		keyMessageMapper
				.saveKeyMessage(new KeyMessage(playerId, messageRef.getId().asString(), KeyMessageType.INVENTORY));
	}

	private void craftingCommand(Long playerDiscId, MessageChannel channel) {

		Integer playerId = userService.getPlayerId(playerDiscId);

		userService.updatePlayerState(playerDiscId, PlayerState.CRAFTING);
		channel.createMessage(userService.getPlayerState(playerDiscId).toString()).block();
		Message messageRef = channel.createMessage("Crafting").block();
		keyMessageMapper
				.saveKeyMessage(new KeyMessage(playerId, messageRef.getId().asString(), KeyMessageType.CRAFTING));
	}
}
