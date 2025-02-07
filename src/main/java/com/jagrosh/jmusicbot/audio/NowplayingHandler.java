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
package com.jagrosh.jmusicbot.audio;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.entities.Pair;
import com.jagrosh.jmusicbot.spring.AppConfiguration;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.stereotype.Component;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@Component
public class NowplayingHandler {
  private final Bot bot;
  private final AppConfiguration config;
  private final HashMap<Long, Pair<Long, Long>> lastNP; // guild -> channel,message

  public NowplayingHandler(Bot bot, AppConfiguration config) {
    this.bot = bot;
    this.config = config;
    this.bot.setNowplaying(this);
    this.lastNP = new HashMap<>();
    if (!config.isNpimages()) {
      bot.getThreadpool().scheduleWithFixedDelay(this::updateAll, 0, 5, TimeUnit.SECONDS);
    }
  }

  public void setLastNPMessage(Message m) {
    lastNP.put(m.getGuild().getIdLong(), new Pair<>(m.getTextChannel().getIdLong(), m.getIdLong()));
  }

  public void clearLastNPMessage(Guild guild) {
    lastNP.remove(guild.getIdLong());
  }

  private void updateAll() {
    Set<Long> toRemove = new HashSet<>();
    for (long guildId : lastNP.keySet()) {
      Guild guild = bot.getJDA().getGuildById(guildId);
      if (guild == null) {
        toRemove.add(guildId);
      } else {
        Pair<Long, Long> pair = lastNP.get(guildId);
        TextChannel tc = guild.getTextChannelById(pair.getKey());
        if (tc == null) {
          toRemove.add(guildId);
        } else {
          AudioHandler handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
          Message msg = handler.getNowPlaying(bot.getJDA());
          if (msg == null) {
            msg = handler.getNoMusicPlaying(bot.getJDA());
            toRemove.add(guildId);
          }
          try {
            tc.editMessageById(pair.getValue(), msg)
                .queue(
                    m -> {
                      // do nothing
                    },
                    t -> lastNP.remove(guildId));
          } catch (Exception e) {
            toRemove.add(guildId);
          }
        }
      }
    }
    toRemove.forEach(lastNP::remove);
  }

  // "event"-based methods
  public void onTrackUpdate(AudioTrack track) {
    // update bot status if applicable
    if (config.isSonginstatus()) {
      if (track != null
          && bot.getJDA().getGuilds().stream()
                  .filter(g -> g.getSelfMember().getVoiceState().inVoiceChannel())
                  .count()
              <= 1)
        bot.getJDA().getPresence().setActivity(Activity.listening(track.getInfo().title));
      else bot.resetGame();
    }
  }

  public void onMessageDelete(Guild guild, long messageId) {
    Pair<Long, Long> pair = lastNP.get(guild.getIdLong());
    if (pair == null) return;
    if (pair.getValue() == messageId) lastNP.remove(guild.getIdLong());
  }
}
