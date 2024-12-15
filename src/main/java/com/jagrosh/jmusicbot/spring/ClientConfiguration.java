package com.jagrosh.jmusicbot.spring;

import com.jagrosh.jmusicbot.jdautils.Command;
import com.jagrosh.jmusicbot.jdautils.CommandClient;
import com.jagrosh.jmusicbot.jdautils.CommandClientBuilder;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.spring.exceptions.IllegalClientConfigurationException;
import java.util.List;
import net.dv8tion.jda.api.OnlineStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfiguration {
  @Bean
  public CommandClient commandClient(
      List<Command> commands, AppConfiguration config, SettingsManager settings)
      throws IllegalClientConfigurationException {
    try {
      // set up the command client
      CommandClientBuilder cb =
          new CommandClientBuilder()
              .setPrefix(config.getPrefix())
              .setAlternativePrefix(config.parseAltPrefix())
              .setOwnerId(Long.toString(config.getOwner()))
              .setEmojis(config.getSuccess(), config.getWarning(), config.getError())
              .setHelpWord(config.getHelp())
              .setLinkedCacheSize(200)
              .setGuildSettingsManager(settings)
              .addCommands(commands);

      // set status if set in config
      if (config.parseStatus() != OnlineStatus.UNKNOWN) cb.setStatus(config.parseStatus());

      // set game
      if (config.parseGame() == null) cb.useDefaultGame();
      else if (config.parseIsGameNone()) cb.setActivity(null);
      else cb.setActivity(config.parseGame());

      return cb.build();
    } catch (IllegalArgumentException ex) {
      throw new IllegalClientConfigurationException(
          "Some aspect of the configuration is " + "invalid: " + ex);
    }
  }
}
