package com.jagrosh.jmusicbot.spring;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.admin.QueueTypeCmd;
import com.jagrosh.jmusicbot.commands.admin.SetdjCmd;
import com.jagrosh.jmusicbot.commands.admin.SettcCmd;
import com.jagrosh.jmusicbot.commands.admin.SetvcCmd;
import com.jagrosh.jmusicbot.commands.admin.SkipratioCmd;
import com.jagrosh.jmusicbot.commands.dj.ForceRemoveCmd;
import com.jagrosh.jmusicbot.commands.dj.ForceskipCmd;
import com.jagrosh.jmusicbot.commands.dj.MoveTrackCmd;
import com.jagrosh.jmusicbot.commands.dj.PauseCmd;
import com.jagrosh.jmusicbot.commands.dj.PlaynextCmd;
import com.jagrosh.jmusicbot.commands.dj.RepeatCmd;
import com.jagrosh.jmusicbot.commands.dj.SkiptoCmd;
import com.jagrosh.jmusicbot.commands.dj.StopCmd;
import com.jagrosh.jmusicbot.commands.dj.VolumeCmd;
import com.jagrosh.jmusicbot.commands.general.SettingsCmd;
import com.jagrosh.jmusicbot.commands.music.NowplayingCmd;
import com.jagrosh.jmusicbot.commands.music.PlayCmd;
import com.jagrosh.jmusicbot.commands.music.PlaylistsCmd;
import com.jagrosh.jmusicbot.commands.music.QueueCmd;
import com.jagrosh.jmusicbot.commands.music.RemoveCmd;
import com.jagrosh.jmusicbot.commands.music.SCSearchCmd;
import com.jagrosh.jmusicbot.commands.music.SearchCmd;
import com.jagrosh.jmusicbot.commands.music.SeekCmd;
import com.jagrosh.jmusicbot.commands.music.ShuffleCmd;
import com.jagrosh.jmusicbot.commands.music.SkipCmd;
import com.jagrosh.jmusicbot.commands.owner.AutoplaylistCmd;
import com.jagrosh.jmusicbot.commands.owner.DebugCmd;
import com.jagrosh.jmusicbot.commands.owner.PlaylistCmd;
import com.jagrosh.jmusicbot.jdautils.CommandClient;
import com.jagrosh.jmusicbot.jdautils.CommandClientBuilder;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.spring.exceptions.IllegalClientConfigurationException;
import net.dv8tion.jda.api.OnlineStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfiguration {
  @Bean
  public CommandClient commandClient(AppConfiguration config, Bot bot, SettingsManager settings)
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
              .addCommands(
                  new SettingsCmd(bot),
                  new NowplayingCmd(bot),
                  new PlayCmd(bot),
                  new PlaylistsCmd(bot),
                  new QueueCmd(bot),
                  new RemoveCmd(bot),
                  new SearchCmd(bot),
                  new SCSearchCmd(bot),
                  new SeekCmd(bot),
                  new ShuffleCmd(bot),
                  new SkipCmd(bot),
                  new ForceRemoveCmd(bot),
                  new ForceskipCmd(bot),
                  new MoveTrackCmd(bot),
                  new PauseCmd(bot),
                  new PlaynextCmd(bot),
                  new RepeatCmd(bot),
                  new SkiptoCmd(bot),
                  new StopCmd(bot),
                  new VolumeCmd(bot),
                  new QueueTypeCmd(bot),
                  new SetdjCmd(bot),
                  new SkipratioCmd(bot),
                  new SettcCmd(bot),
                  new SetvcCmd(bot),
                  new AutoplaylistCmd(bot),
                  new DebugCmd(bot),
                  new PlaylistCmd(bot));

      // set status if set in config
      if (config.parseStatus() != OnlineStatus.UNKNOWN) cb.setStatus(config.parseStatus());

      // set game
      if (config.parseGame() == null) cb.useDefaultGame();
      else if (config.parseIsGameNone()) cb.setActivity(null);
      else cb.setActivity(config.parseGame());

      return cb.build();
    } catch (IllegalArgumentException ex) {
      throw new IllegalClientConfigurationException(
          "Some aspect of the configuration is "
              + "invalid: "
              + ex);
    }
  }
}
