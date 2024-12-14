/*
 * Copyright 2016 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot;

import ch.qos.logback.classic.Level;
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
import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.jdautils.CommandClient;
import com.jagrosh.jmusicbot.jdautils.CommandClientBuilder;
import com.jagrosh.jmusicbot.jdautils.utils.EventWaiter;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import java.util.Arrays;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author John Grosh (jagrosh)
 */
public class JMusicBot {
  public static final Logger LOG = LoggerFactory.getLogger(JMusicBot.class);
  public static final Permission[] RECOMMENDED_PERMS = {
    Permission.MESSAGE_READ,
    Permission.MESSAGE_WRITE,
    Permission.MESSAGE_HISTORY,
    Permission.MESSAGE_ADD_REACTION,
    Permission.MESSAGE_EMBED_LINKS,
    Permission.MESSAGE_ATTACH_FILES,
    Permission.MESSAGE_MANAGE,
    Permission.MESSAGE_EXT_EMOJI,
    Permission.VOICE_CONNECT,
    Permission.VOICE_SPEAK,
    Permission.NICKNAME_CHANGE
  };
  public static final GatewayIntent[] INTENTS = {
    GatewayIntent.DIRECT_MESSAGES,
    GatewayIntent.GUILD_MESSAGES,
    GatewayIntent.GUILD_MESSAGE_REACTIONS,
    GatewayIntent.GUILD_VOICE_STATES
  };

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    startBot();
  }

  private static void startBot() {
    // create prompt to handle startup
    Prompt prompt = new Prompt("JMusicBot");

    // startup checks
    OtherUtil.checkJavaVersion(prompt);

    // load config
    BotConfig config = new BotConfig(prompt);
    config.load();
    if (!config.isValid()) return;
    LOG.info("Loaded config from " + config.getConfigLocation());

    // set log level from config
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
        .setLevel(Level.toLevel(config.getLogLevel(), Level.INFO));

    // set up the listener
    EventWaiter waiter = new EventWaiter();
    SettingsManager settings = new SettingsManager();
    Bot bot = new Bot(waiter, config, settings);
    CommandClient client = createCommandClient(config, settings, bot);

    // attempt to log in and start
    try {
      JDA jda =
          JDABuilder.create(config.getToken(), Arrays.asList(INTENTS))
              .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
              .disableCache(
                  CacheFlag.ACTIVITY,
                  CacheFlag.CLIENT_STATUS,
                  CacheFlag.EMOTE,
                  CacheFlag.ONLINE_STATUS)
              .setActivity(config.isGameNone() ? null : Activity.playing("loading..."))
              .setStatus(
                  config.getStatus() == OnlineStatus.INVISIBLE
                          || config.getStatus() == OnlineStatus.OFFLINE
                      ? OnlineStatus.INVISIBLE
                      : OnlineStatus.DO_NOT_DISTURB)
              .addEventListeners(client, waiter, new Listener(bot))
              .setBulkDeleteSplittingEnabled(true)
              .build();
      bot.setJDA(jda);

      // check if something about the current startup is not supported
      String unsupportedReason = OtherUtil.getUnsupportedBotReason(jda);
      if (unsupportedReason != null) {
        prompt.alert(
            Prompt.Level.ERROR,
            "JMusicBot",
            "JMusicBot cannot be run on this Discord bot: " + unsupportedReason);
        try {
          Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        } // this is awful but until we have a better way...
        jda.shutdown();
        System.exit(1);
      }

      // other check that will just be a warning now but may be required in the future
      // check if the user has changed the prefix and provide info about the
      // message content intent
      if (!"@mention".equals(config.getPrefix())) {
        LOG.info(
            "JMusicBot",
            "You currently have a custom prefix set. "
                + "If your prefix is not working, make sure that the 'MESSAGE CONTENT INTENT' is Enabled "
                + "on https://discord.com/developers/applications/"
                + jda.getSelfUser().getId()
                + "/bot");
      }
    } catch (LoginException ex) {
      prompt.alert(
          Prompt.Level.ERROR,
          "JMusicBot",
          ex
              + "\nPlease make sure you are "
              + "editing the correct config.txt file, and that you have used the "
              + "correct token (not the 'secret'!)\nConfig Location: "
              + config.getConfigLocation());
      System.exit(1);
    } catch (IllegalArgumentException ex) {
      prompt.alert(
          Prompt.Level.ERROR,
          "JMusicBot",
          "Some aspect of the configuration is "
              + "invalid: "
              + ex
              + "\nConfig Location: "
              + config.getConfigLocation());
      System.exit(1);
    } catch (ErrorResponseException ex) {
      prompt.alert(
          Prompt.Level.ERROR,
          "JMusicBot",
          ex
              + "\nInvalid reponse returned when "
              + "attempting to connect, please make sure you're connected to the internet");
      System.exit(1);
    }
  }

  private static CommandClient createCommandClient(
      BotConfig config, SettingsManager settings, Bot bot) {

    // set up the command client
    CommandClientBuilder cb =
        new CommandClientBuilder()
            .setPrefix(config.getPrefix())
            .setAlternativePrefix(config.getAltPrefix())
            .setOwnerId(Long.toString(config.getOwnerId()))
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
    if (config.getStatus() != OnlineStatus.UNKNOWN) cb.setStatus(config.getStatus());

    // set game
    if (config.getGame() == null) cb.useDefaultGame();
    else if (config.isGameNone()) cb.setActivity(null);
    else cb.setActivity(config.getGame());

    return cb.build();
  }
}
