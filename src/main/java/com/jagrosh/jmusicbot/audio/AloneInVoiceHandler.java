/*
 * Copyright 2021 John Grosh <john.a.grosh@gmail.com>.
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
import com.jagrosh.jmusicbot.spring.AppConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import org.springframework.stereotype.Component;

/**
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
@Component
public class AloneInVoiceHandler {
  private final Bot bot;
  private final HashMap<Long, Instant> aloneSince = new HashMap<>();
  private long aloneTimeUntilStop = 0;

  public AloneInVoiceHandler(Bot bot, AppConfiguration config) {
    this.bot = bot;
    this.bot.setAloneInVoiceHandler(this);
    aloneTimeUntilStop = config.getAlonetimeuntilstop();
    if (aloneTimeUntilStop > 0) {
      bot.getThreadpool().scheduleWithFixedDelay(this::check, 0, 5, TimeUnit.SECONDS);
    }
  }

  private void check() {
    final var toRemove = new HashSet<Long>();
    for (Map.Entry<Long, Instant> entry : aloneSince.entrySet()) {
      final var aloneSecondsSince = entry.getValue().until(Instant.now(), ChronoUnit.SECONDS);
      if (aloneSecondsSince > aloneTimeUntilStop) {
        final var guild = bot.getJDA().getGuildById(entry.getKey());

        if (guild != null
            && guild.getAudioManager().getSendingHandler() instanceof AudioHandler audioHandler) {
          audioHandler.stopAndClear();
          guild.getAudioManager().closeAudioConnection();
        }
        toRemove.add(entry.getKey());
      }
    }
    toRemove.forEach(aloneSince::remove);
  }

  public void onVoiceUpdate(GuildVoiceUpdateEvent event) {
    if (aloneTimeUntilStop <= 0) return;

    Guild guild = event.getEntity().getGuild();
    if (!bot.getPlayerManager().hasHandler(guild)) return;

    boolean alone = isAlone(guild);
    boolean inList = aloneSince.containsKey(guild.getIdLong());

    if (!alone && inList) aloneSince.remove(guild.getIdLong());
    else if (alone && !inList) aloneSince.put(guild.getIdLong(), Instant.now());
  }

  private boolean isAlone(Guild guild) {
    if (guild.getAudioManager().getConnectedChannel() == null) {
      return false;
    }
    return guild.getAudioManager().getConnectedChannel().getMembers().stream()
        .noneMatch(x -> !x.getVoiceState().isDeafened() && !x.getUser().isBot());
  }
}
