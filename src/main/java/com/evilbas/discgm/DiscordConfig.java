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
import discord4j.core.object.entity.User;

@Configuration
public class DiscordConfig {
    private Logger log = LoggerFactory.getLogger(DiscordConfig.class);

    @Autowired
    private Environment env;

    @Bean
    public GatewayDiscordClient discordClient() {
        log.debug("init discord client");
        GatewayDiscordClient client = DiscordClientBuilder.create(env.getProperty("disc.token")).build().login()
                .block();

        client.getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            final User self = event.getSelf();
            log.info(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
        });
        return client;
    }
}
