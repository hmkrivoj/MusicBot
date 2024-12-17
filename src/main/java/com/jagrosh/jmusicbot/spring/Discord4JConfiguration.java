package com.jagrosh.jmusicbot.spring;

import com.jagrosh.jmusicbot.spring.exceptions.ConnectErrorException;
import com.jagrosh.jmusicbot.spring.exceptions.UnsupportedBotException;
import discord4j.common.store.Store;
import discord4j.common.store.api.StoreFlag;
import discord4j.common.store.impl.LocalStoreLayout;
import discord4j.common.store.impl.SelectiveStoreLayout;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Discord4JConfiguration {
  private static final Intent[] INTENTS = {
    Intent.DIRECT_MESSAGES,
    Intent.GUILD_MESSAGES,
    Intent.GUILD_MESSAGE_REACTIONS,
    Intent.GUILD_VOICE_STATES,
  };
  private static final Logger log = LoggerFactory.getLogger(Discord4JConfiguration.class);

  @Bean
  public GatewayDiscordClient discordClient(AppConfiguration config)
      throws ConnectErrorException, UnsupportedBotException {
    final var selectiveStoreLayout =
        SelectiveStoreLayout.create(
            EnumSet.of(StoreFlag.VOICE_STATE, StoreFlag.MEMBER), LocalStoreLayout.create());
    final var client =
        DiscordClient.create(config.getToken())
            .gateway()
            .setEnabledIntents(IntentSet.of(INTENTS))
            .setStore(Store.fromLayout(selectiveStoreLayout))
            .setInitialPresence(
                s -> {
                  var status = parseStatus(config.getStatus());
                  if (status == Status.UNKNOWN) {
                    log.warn(
                        "Unrecognized status '{}'. Using 'ONLINE' instead.", config.getStatus());
                    status = Status.ONLINE;
                  }
                  // TODO split activity and game
                  final var activity = parseActivity(config.getGame());
                  return ClientPresence.of(status, activity);
                })
            .login()
            .block();

    if (client == null) {
      throw new ConnectErrorException(
          "Unable to obtain Discord Client. Please check your connection and make sure system property 'app.token' is set.");
    }
    try {
      checkIfBotSupported(client);
    } catch (UnsupportedBotException e) {
      client.logout().block();
      throw e;
    }
    return client;
  }

  private static Status parseStatus(String rawStatus) {
    if (rawStatus == null || rawStatus.trim().isEmpty()) {
      return Status.ONLINE;
    }
    return switch (rawStatus.trim().toUpperCase()) {
      case "ONLINE" -> Status.ONLINE;
      case "IDLE" -> Status.IDLE;
      case "DND", "DO_NOT_DISTURB", "DO-NOT-DISTURB", "DONOTDISTURB" -> Status.DO_NOT_DISTURB;
      case "INVISIBLE" -> Status.INVISIBLE;
      default -> Status.UNKNOWN;
    };
  }

  private static ClientActivity parseActivity(String rawActivity) {
    // TODO split activity and game
    return ClientActivity.listening("TODO");
  }

  private static void checkIfBotSupported(GatewayDiscordClient client)
      throws ConnectErrorException, UnsupportedBotException {
    final var selfUser = client.getSelf().block();
    if (selfUser == null) {
      throw new ConnectErrorException(
          "Unable to obtain Bot User. Please check your connection and make sure system property 'app.token' is set.");
    }
    if (selfUser.getPublicFlags().contains(User.Flag.VERIFIED_BOT)) {
      throw new UnsupportedBotException(
          "The bot is verified. Using JMusicBot in a verified bot is not supported.");
    }
    final var applicationInfo = client.getApplicationInfo().block();
    if (applicationInfo == null) {
      throw new ConnectErrorException(
          "Unable to obtain application info.  Please check your connection and make sure system property 'app.token' is set.");
    }
    if (applicationInfo.isPublic()) {
      throw new UnsupportedBotException(
          "\"Public Bot\" is enabled. Using JMusicBot as a public bot is not supported. Please disable it in the "
              + "Developer Dashboard at https://discord.com/developers/applications/"
              + selfUser.getId()
              + "/bot ."
              + "You may also need to disable all Installation Contexts at https://discord.com/developers/applications/"
              + selfUser.getId()
              + "/installation .");
    }
  }
}
