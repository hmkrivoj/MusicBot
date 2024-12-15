package com.jagrosh.jmusicbot.spring;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.jdautils.CommandClient;
import com.jagrosh.jmusicbot.jdautils.utils.EventWaiter;
import com.jagrosh.jmusicbot.spring.exceptions.ConnectErrorException;
import com.jagrosh.jmusicbot.spring.exceptions.TokenException;
import com.jagrosh.jmusicbot.spring.exceptions.UnsupportedBotException;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import java.util.Arrays;
import javax.security.auth.login.LoginException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JdaConfiguration {
  public static final GatewayIntent[] INTENTS = {
    GatewayIntent.DIRECT_MESSAGES,
    GatewayIntent.GUILD_MESSAGES,
    GatewayIntent.GUILD_MESSAGE_REACTIONS,
    GatewayIntent.GUILD_VOICE_STATES
  };

  @Bean
  public JDA jda(CommandClient client, AppConfiguration config, EventWaiter waiter, Bot bot)
      throws TokenException, UnsupportedBotException, ConnectErrorException {
    JDA jda = null;
    try {
      jda =
          JDABuilder.create(config.getToken(), Arrays.asList(INTENTS))
              .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
              .disableCache(
                  CacheFlag.ACTIVITY,
                  CacheFlag.CLIENT_STATUS,
                  CacheFlag.EMOTE,
                  CacheFlag.ONLINE_STATUS)
              .setActivity(config.parseIsGameNone() ? null : Activity.playing("loading..."))
              .setStatus(
                  config.parseStatus() == OnlineStatus.INVISIBLE
                          || config.parseStatus() == OnlineStatus.OFFLINE
                      ? OnlineStatus.INVISIBLE
                      : OnlineStatus.DO_NOT_DISTURB)
              .addEventListeners(client, waiter, bot)
              .setBulkDeleteSplittingEnabled(true)
              .build();
      bot.setJDA(jda);
      // check if something about the current startup is not supported
      OtherUtil.checkIfBotSupported(jda);

      // other check that will just be a warning now but may be required in the future
      // check if the user has changed the prefix and provide info about the
      // message content intent
      if (!"@mention".equals(config.getPrefix())) {
        log.info(
            "JMusicBot",
            "You currently have a custom prefix set. "
                + "If your prefix is not working, make sure that the 'MESSAGE CONTENT INTENT' is Enabled "
                + "on https://discord.com/developers/applications/"
                + jda.getSelfUser().getId()
                + "/bot");
      }

      return jda;
    } catch (LoginException e) {
      throw new TokenException(
          e
              + "\nPlease make sure you are "
              + "editing the correct config.txt file, and that you have used the "
              + "correct token (not the 'secret'!)");
    } catch (UnsupportedBotException e) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      jda.shutdown();
      throw e;
    } catch (ErrorResponseException ex) {
      throw new ConnectErrorException(
          ex
              + "\nInvalid reponse returned when "
              + "attempting to connect, please make sure you're connected to the internet");
    }
  }
}
