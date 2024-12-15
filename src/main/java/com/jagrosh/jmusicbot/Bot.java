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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
@Component
public class Bot {
  @Getter private final EventWaiter waiter;
  @Getter private final ScheduledExecutorService threadpool;
  @Getter private final AppConfiguration config;
  private final SettingsManager settings;
  private final PlayerManager players;
  private final PlaylistLoader playlists;
  private final NowplayingHandler nowplaying;
  @Getter private final AloneInVoiceHandler aloneInVoiceHandler;

  private boolean shuttingDown = false;
  private JDA jda;

  @Autowired
  public Bot(EventWaiter waiter, AppConfiguration config, SettingsManager settings) {
    this.waiter = waiter;
    this.config = config;
    this.settings = settings;
    this.playlists = new PlaylistLoader(config);
    this.threadpool = Executors.newSingleThreadScheduledExecutor();
    this.players = new PlayerManager(this);
    this.players.init();
    this.nowplaying = new NowplayingHandler(this);
    this.nowplaying.init();
    this.aloneInVoiceHandler = new AloneInVoiceHandler(this);
    this.aloneInVoiceHandler.init();
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
}
