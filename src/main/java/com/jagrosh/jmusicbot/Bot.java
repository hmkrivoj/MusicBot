/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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

import com.jagrosh.jmusicbot.audio.AloneInVoiceHandler;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.NowplayingHandler;
import com.jagrosh.jmusicbot.audio.PlayerManager;
import com.jagrosh.jmusicbot.jdautils.utils.EventWaiter;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.spring.AppConfiguration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
@Component
public class Bot extends ListenerAdapter {
  @Getter private final EventWaiter waiter;
  @Getter private final ScheduledExecutorService threadpool;
  private final AppConfiguration config;
  private final SettingsManager settings;
  @Setter private PlayerManager players;
  private final PlaylistLoader playlists;
  @Setter private NowplayingHandler nowplaying;
  @Getter @Setter private AloneInVoiceHandler aloneInVoiceHandler;

  private boolean shuttingDown = false;
  private JDA jda;

  @Autowired
  public Bot(EventWaiter waiter, AppConfiguration config, SettingsManager settings) {
    this.waiter = waiter;
    this.config = config;
    this.settings = settings;
    this.playlists = new PlaylistLoader(config);
    this.threadpool = Executors.newSingleThreadScheduledExecutor();
  }

  public SettingsManager getSettingsManager() {
    return settings;
  }

  public PlayerManager getPlayerManager() {
    return players;
  }

  public PlaylistLoader getPlaylistLoader() {
    return playlists;
  }

  public NowplayingHandler getNowplayingHandler() {
    return nowplaying;
  }

  public JDA getJDA() {
    return jda;
  }

  public void closeAudioConnection(long guildId) {
    Guild guild = jda.getGuildById(guildId);
    if (guild != null) threadpool.submit(() -> guild.getAudioManager().closeAudioConnection());
  }

  public void resetGame() {
    Activity game =
        config.parseGame() == null || config.parseGame().getName().equalsIgnoreCase("none")
            ? null
            : config.parseGame();
    if (!Objects.equals(jda.getPresence().getActivity(), game)) jda.getPresence().setActivity(game);
  }

  public void shutdown() {
    if (shuttingDown) return;
    shuttingDown = true;
    threadpool.shutdownNow();
    if (jda.getStatus() != JDA.Status.SHUTTING_DOWN) {
      jda.getGuilds().stream()
          .forEach(
              g -> {
                g.getAudioManager().closeAudioConnection();
                AudioHandler ah = (AudioHandler) g.getAudioManager().getSendingHandler();
                if (ah != null) {
                  ah.stopAndClear();
                  ah.getPlayer().destroy();
                }
              });
      jda.shutdown();
    }
    System.exit(0);
  }

  public void setJDA(JDA jda) {
    this.jda = jda;
  }

  @Override
  public void onReady(ReadyEvent event) {
    if (event.getJDA().getGuildCache().isEmpty()) {
      Logger log = LoggerFactory.getLogger("MusicBot");
      log.warn(
          "This bot is not on any guilds! Use the following link to add the bot to your guilds!\n{}",
          event.getJDA().getInviteUrl(JMusicBot.RECOMMENDED_PERMS));
    }
    event
        .getJDA()
        .getGuilds()
        .forEach(
            guild -> {
              try {
                String defpl = getSettingsManager().getSettings(guild).getDefaultPlaylist();
                VoiceChannel vc = getSettingsManager().getSettings(guild).getVoiceChannel(guild);
                if (defpl != null
                    && vc != null
                    && getPlayerManager().getOrCreateAudioHandler(guild).playFromDefault()) {
                  guild.getAudioManager().openAudioConnection(vc);
                }
              } catch (Exception ignore) {
                // do nothing
              }
            });
    User owner = getJDA().retrieveUserById(config.getOwner()).complete();
    if (owner != null) {
      final var msg = "JMusicBot is running";
      owner.openPrivateChannel().queue(pc -> pc.sendMessage(msg).queue());
    }
  }

  @Override
  public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
    getNowplayingHandler().onMessageDelete(event.getGuild(), event.getMessageIdLong());
  }

  @Override
  public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
    getAloneInVoiceHandler().onVoiceUpdate(event);
  }

  @Override
  public void onShutdown(ShutdownEvent event) {
    shutdown();
  }
}
