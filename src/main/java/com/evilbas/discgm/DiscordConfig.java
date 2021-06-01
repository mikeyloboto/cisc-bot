package com.evilbas.discgm;

import java.util.Map;

import com.evilbas.discgm.discord.CommandHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.User;

import com.evilbas.discgm.discord.domain.MessageCommand;
import com.evilbas.discgm.util.Constants;

@Configuration
public class DiscordConfig {
    private Logger log = LoggerFactory.getLogger(DiscordConfig.class);

    @Autowired
    private Environment env;

    @Autowired
    private CommandHandler commandHandler;

    @Bean
    public GatewayDiscordClient discordClient() {
        log.debug("init discord client");
        GatewayDiscordClient client = DiscordClientBuilder.create(env.getProperty("disc.token")).build().login()
                .block();
        client.getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            final User self = event.getSelf();
            System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
        });

        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            final String content = event.getMessage().getContent();

            for (final Map.Entry<String, MessageCommand> entry : commandHandler.getCommands().entrySet()) {
                if (content.startsWith(Constants.DISC_BOT_PREFIX + entry.getKey())) {
                    entry.getValue().execute(event);
                    break;
                }
            }
        });

        client.onDisconnect().block();
        return client;
    }
}
