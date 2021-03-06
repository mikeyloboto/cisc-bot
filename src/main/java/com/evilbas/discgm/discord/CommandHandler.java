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
import discord4j.core.spec.EmbedCreateSpec;
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
import com.evilbas.rslengine.ability.Spell;
import com.evilbas.rslengine.ability.Spellbook;
import com.evilbas.rslengine.character.Character;
import com.evilbas.rslengine.creature.Creature;
import com.evilbas.rslengine.creature.Encounter;
import com.evilbas.rslengine.damage.DamageModifier;
import com.evilbas.rslengine.item.Inventory;
import com.evilbas.rslengine.item.ItemStack;
import com.evilbas.rslengine.item.property.ItemRarity;
import com.evilbas.rslengine.networking.CombatResultWrapper;
import com.evilbas.rslengine.networking.InventoryInteractionWrapper;
import com.evilbas.rslengine.networking.SpellbookInteractionWrapper;
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

			String playerName = event.getMessage().getAuthor().get().getUsername();

			userService.loadUser(playerId, playerName);

			for (final Map.Entry<String, MessageCommand> entry : getCommands().entrySet()) {
				if (content.toLowerCase().startsWith(Constants.DISC_BOT_PREFIX + entry.getKey())) {
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
						case "??????":
							combatCommand(playerId, event.getMessage().block().getChannel().block());
							break;
						case "????":
							inventoryCommand(playerId, event.getMessage().block().getChannel().block());
							break;
						case "????":
							spellbookCommand(playerId, event.getMessage().block().getChannel().block());
							break;
						case "???????":
							craftingCommand(playerId, event.getMessage().block().getChannel().block());
							break;
					}
					break;
				case COMBAT:
					if (event.getEmoji().asUnicodeEmoji().get().getRaw().equals("??????")) {
						// autoattack case
						attackCommand(playerId, event.getMessage().block().getChannel().block(), null,
								event.getMessage().block());
					}
					break;
				case INVENTORY:
					useItemCommand(playerId, event.getMessage().block().getChannel().block(), null,
							event.getMessage().block(), event.getEmoji().asUnicodeEmoji().get().getRaw());
					break;
				case SPELLBOOK:
					useSpellCommand(playerId, event.getMessage().block().getChannel().block(), null,
							event.getMessage().block(), event.getEmoji().asUnicodeEmoji().get().getRaw());
					break;
			}

		});

		// discordClient.getEventDispatcher().on()

		commands = new HashMap<>();
		commands.put("ping", event -> event.getMessage().getChannel().block().createMessage("Pong!").block());
		commands.put("start", event -> {

			event.getMessage().getChannel().block().createMessage("Pong!").block();
		});
		commands.put("status", event -> {

			if (event.getMessage().getChannel().block().getType().equals(Channel.Type.DM)) {
				Long playerDiscId = event.getMessage().getAuthor().get().getId().asLong();
				statusCommand(playerDiscId, event.getMessage().getChannel().block());
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
		CombatResultWrapper actionResult = procServerClient
				.retrieveEncounter(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid());
		Encounter encounter = actionResult.getEncounter();

		Mono<Message> combatMessageRefBuild = channel.createEmbed(spec -> {
			spec.setColor(Color.BLUE).setAuthor("CISC Bot", "", "").setTitle("Combat").setDescription("Enemies: "
					+ encounter.getCreatures().size() + "\n" + "Encounter Exp: " + encounter.getEncounterExp(character))
					.setTimestamp(Instant.now());
			for (Creature c : encounter.getCreatures()) {
				spec.addField("Lvl " + c.getLevel() + " " + c.getName(), c.getCurrentHp() + "/" + c.getMaxHp(), false);
			}
		});

		Message combatMessageRef = combatMessageRefBuild.block();
		combatMessageRef.addReaction(ReactionEmoji.unicode("??????")).block();

		keyMessageMapper
				.saveKeyMessage(new KeyMessage(playerId, combatMessageRef.getId().asString(), KeyMessageType.COMBAT));
	}

	private void attackCommand(Long playerDiscId, MessageChannel channel, String attack, Message oldMessage) {
		oldMessage.delete().block();
		Integer playerId = userService.getPlayerId(playerDiscId);

		// TODO: adjust spell slots
		CombatResultWrapper result = procServerClient
				.performAttack(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid(), 0, 0);
		for (String m : result.getMessages())
			channel.createMessage(m).block();
		if (result.isFinished()) {
			userService.updatePlayerState(playerDiscId, PlayerState.IDLE);
			statusCommand(playerDiscId, channel);

		} else
			combatCommand(playerDiscId, channel);

	}

	private void useItemCommand(Long playerDiscId, MessageChannel channel, String attack, Message oldMessage,
			String item) {
		oldMessage.delete().block();
		Integer playerId = userService.getPlayerId(playerDiscId);
		InventoryInteractionWrapper result = procServerClient
				.listInventory(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid());

		Inventory inventory = result.getInventory();

		boolean itemValid = false;
		for (ItemStack i : inventory.getItems()) {
			if (i.getItem().getIcon().equals(item))
				itemValid = true;
		}

		if (itemValid) {
			log.info("Using item {}", item);
			InventoryInteractionWrapper itemResult = procServerClient
					.useItem(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid(), item);

			channel.createMessage(item + " used.").block();

			if (itemResult.getMessages() != null)
				for (String m : itemResult.getMessages()) {
					channel.createMessage(m).block();

				}
		}

		inventoryCommand(playerDiscId, channel);

	}

	private void useSpellCommand(Long playerDiscId, MessageChannel channel, String attack, Message oldMessage,
			String spell) {
		oldMessage.delete().block();
		Integer playerId = userService.getPlayerId(playerDiscId);
		SpellbookInteractionWrapper result = procServerClient
				.listSpellbook(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid());

		Spellbook spellbook = result.getSpellbook();

		boolean spellValid = false;
		for (Spell i : spellbook.getSpells()) {
			if (i.getIcon().equals(spell))
				spellValid = true;
		}

		if (spellValid) {
			log.info("Using spell {}", spell);
			SpellbookInteractionWrapper spellResult = procServerClient
					.useSpell(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid(), spell);

			if (spellResult.getMessages() != null)
				for (String m : spellResult.getMessages()) {
					channel.createMessage(m).block();
				}
		}

		spellbookCommand(playerDiscId, channel);
	}

	private void inventoryCommand(Long playerDiscId, MessageChannel channel) {

		Integer playerId = userService.getPlayerId(playerDiscId);
		InventoryInteractionWrapper result = procServerClient
				.listInventory(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid());

		Inventory inventory = result.getInventory();

		Mono<Message> inventoryMessageRefBuild = channel.createEmbed(spec -> {
			spec.setColor(Color.BLUE).setAuthor("CISC Bot", "", "").setTitle("Inventory").setTimestamp(Instant.now());
			for (ItemStack i : inventory.getItems()) {
				spec.addField(i.getItem().getIcon() + " " + i.getItem().getName(), "Amount: " + i.getAmount().toString()
						+ ". " + ItemRarity.toReadable(i.getItem().getRarity()) + " item.", false);
			}
		});

		Message inventoryMessageRef = inventoryMessageRefBuild.block();
		for (ItemStack i : inventory.getItems()) {
			inventoryMessageRef.addReaction(ReactionEmoji.unicode(i.getItem().getIcon())).block();
		}

		keyMessageMapper.saveKeyMessage(
				new KeyMessage(playerId, inventoryMessageRef.getId().asString(), KeyMessageType.INVENTORY));
	}

	private void spellbookCommand(Long playerDiscId, MessageChannel channel) {
		Integer playerId = userService.getPlayerId(playerDiscId);
		SpellbookInteractionWrapper result = procServerClient
				.listSpellbook(characterService.getActiveCharacterForPlayer(playerId).getCharacterGuid());

		Spellbook spellbook = result.getSpellbook();

		Mono<Message> inventoryMessageRefBuild = channel.createEmbed(spec -> {
			spec.setColor(Color.RED).setAuthor("CISC Bot", "", "").setTitle("Spellbook").setTimestamp(Instant.now());
			for (Spell i : spellbook.getSpells()) {
				spec.addField(i.getIcon() + " " + i.getSpellName(),
						"MP: " + i.getManaCost() + ".\n" + i.getDescription(), false);
			}
		});

		Message inventoryMessageRef = inventoryMessageRefBuild.block();
		for (Spell i : spellbook.getSpells()) {
			inventoryMessageRef.addReaction(ReactionEmoji.unicode(i.getIcon())).block();
		}

		keyMessageMapper.saveKeyMessage(
				new KeyMessage(playerId, inventoryMessageRef.getId().asString(), KeyMessageType.SPELLBOOK));
	}

	private void craftingCommand(Long playerDiscId, MessageChannel channel) {

		Integer playerId = userService.getPlayerId(playerDiscId);

		userService.updatePlayerState(playerDiscId, PlayerState.CRAFTING);
		channel.createMessage(userService.getPlayerState(playerDiscId).toString()).block();
		Message messageRef = channel.createMessage("Crafting").block();
		keyMessageMapper
				.saveKeyMessage(new KeyMessage(playerId, messageRef.getId().asString(), KeyMessageType.CRAFTING));
	}

	private void statusCommand(Long playerDiscId, MessageChannel channel) {

		Integer playerId = userService.getPlayerId(playerDiscId);
		Character character = characterService.getActiveCharacterForPlayer(playerId);
		Message statusMessageRef = channel.createEmbed(spec -> {
			spec.setColor(Color.BLUE).setAuthor(character.getCharacterName(), "", "").setTitle("Status")
					.setDescription("Level: " + character.getCharacterLevel() + "\n" + "Current Exp: "
							+ character.getCharacterExp())
					.addField("Level", character.getCharacterLevel().toString(), true)
					.addField("Exp", character.getCharacterExp().toString(), true).addField("Class", "Warrior", true)
					.addField("HP", character.getCurrentHp() + "/" + character.getMaxHp(), true)
					.addField("MP", character.getMp() + "/" + character.getMaxMp(), true);
			if (character.getEquippedWeapon() != null) {
				String weaponInfo = character.getEquippedWeapon().getName() + "\nBase attack: "
						+ character.getEquippedWeapon().getBaseDamage();
				if (character.getEquippedWeapon().getModifiers() != null) {
					for (DamageModifier mod : character.getEquippedWeapon().getModifiers()) {
						weaponInfo += ("\n+" + mod.getAmount() + " " + mod.getDamageType().getReadableName(false)
								+ " damage");
					}
				}
				spec.addField("Equipped Weapon", weaponInfo, true);

			}

			if (character.getEquippedArmor() != null) {
				String armorInfo = character.getEquippedArmor().getName() + "\nBase armor: "
						+ character.getEquippedArmor().getBaseArmor();
				if (character.getEquippedArmor().getModifiers() != null) {
					for (DamageModifier mod : character.getEquippedArmor().getModifiers()) {
						armorInfo += ("\n+" + mod.getAmount() + " " + mod.getDamageType().getReadableName(false)
								+ " armor");
					}
				}
				spec.addField("Equipped Armor", armorInfo, true);

			}
			spec.setTimestamp(Instant.now()).addField("Currently",
					CharacterUtils.stateToDesciption(userService.getPlayerState(playerDiscId))
							+ ((userService.getPlayerState(playerDiscId) == PlayerState.COMBAT)
									? " " + character.getCurrentEncounter().getCreatureSlot(0).getName()
									: ""),
					true);
		}).block();

		// Message statusMessageRef = embedSpec.block();
		statusMessageRef.addReaction(ReactionEmoji.unicode("??????")).block();
		statusMessageRef.addReaction(ReactionEmoji.unicode("????")).block();
		statusMessageRef.addReaction(ReactionEmoji.unicode("????")).block();
		statusMessageRef.addReaction(ReactionEmoji.unicode("????")).block();
		statusMessageRef.addReaction(ReactionEmoji.unicode("???????")).block();
		keyMessageMapper
				.saveKeyMessage(new KeyMessage(playerId, statusMessageRef.getId().asString(), KeyMessageType.STATUS));
		log.info("Message: {}", statusMessageRef.getId().asString());
	}
}
