/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.jdautils.CommandEvent;
import com.jagrosh.jmusicbot.jdautils.utils.OrderedMenu;
import com.jagrosh.jmusicbot.spring.AppConfiguration;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.stereotype.Component;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
@Component
public class SearchCmd extends MusicCommand {
  protected String searchPrefix = "ytsearch:";
  private final OrderedMenu.Builder builder;
  private final String searchingEmoji;
  private final AppConfiguration config;

  public SearchCmd(Bot bot, AppConfiguration config) {
    super(bot);
    this.config = config;
    this.searchingEmoji = config.getSearching();
    this.name = "search";
    this.aliases = config.getAliases().get(this.name);
    this.arguments = "<query>";
    this.help = "searches Youtube for a provided query";
    this.beListening = true;
    this.bePlaying = false;
    this.botPermissions = new Permission[] {Permission.MESSAGE_EMBED_LINKS};
    builder =
        new OrderedMenu.Builder()
            .allowTextInput(true)
            .useNumbers()
            .useCancelButton(true)
            .setEventWaiter(bot.getWaiter())
            .setTimeout(1, TimeUnit.MINUTES);
  }

  @Override
  public void doCommand(CommandEvent event) {
    if (event.getArgs().isEmpty()) {
      event.replyError("Please include a query.");
      return;
    }
    event.reply(
        searchingEmoji + " Searching... `[" + event.getArgs() + "]`",
        m ->
            bot.getPlayerManager()
                .loadItemOrdered(
                    event.getGuild(), searchPrefix + event.getArgs(), new ResultHandler(m, event)));
  }

  private class ResultHandler implements AudioLoadResultHandler {
    private final Message m;
    private final CommandEvent event;

    private ResultHandler(Message m, CommandEvent event) {
      this.m = m;
      this.event = event;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
      if (config.calcIsTooLong(track)) {
        m.editMessage(
                FormatUtil.filter(
                    event.getClient().getWarning()
                        + " This track (**"
                        + track.getInfo().title
                        + "**) is longer than the allowed maximum: `"
                        + TimeUtil.formatTime(track.getDuration())
                        + "` > `"
                        + config.getMaxtime()
                        + "`"))
            .queue();
        return;
      }
      AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
      int pos =
          handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event)))
              + 1;
      m.editMessage(
              FormatUtil.filter(
                  event.getClient().getSuccess()
                      + " Added **"
                      + track.getInfo().title
                      + "** (`"
                      + TimeUtil.formatTime(track.getDuration())
                      + "`) "
                      + (pos == 0 ? "to begin playing" : " to the queue at position " + pos)))
          .queue();
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
      builder
          .setColor(event.getSelfMember().getColor())
          .setText(
              FormatUtil.filter(
                  event.getClient().getSuccess()
                      + " Search results for `"
                      + event.getArgs()
                      + "`:"))
          .setChoices()
          .setSelection(
              (msg, i) -> {
                AudioTrack track = playlist.getTracks().get(i - 1);
                if (config.calcIsTooLong(track)) {
                  event.replyWarning(
                      "This track (**"
                          + track.getInfo().title
                          + "**) is longer than the allowed maximum: `"
                          + TimeUtil.formatTime(track.getDuration())
                          + "` > `"
                          + config.getMaxtime()
                          + "`");
                  return;
                }
                AudioHandler handler =
                    (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                int pos =
                    handler.addTrack(
                            new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event)))
                        + 1;
                event.replySuccess(
                    "Added **"
                        + FormatUtil.filter(track.getInfo().title)
                        + "** (`"
                        + TimeUtil.formatTime(track.getDuration())
                        + "`) "
                        + (pos == 0 ? "to begin playing" : " to the queue at position " + pos));
              })
          .setCancel(msg -> {})
          .setUsers(event.getAuthor());
      for (int i = 0; i < 4 && i < playlist.getTracks().size(); i++) {
        AudioTrack track = playlist.getTracks().get(i);
        builder.addChoices(
            "`["
                + TimeUtil.formatTime(track.getDuration())
                + "]` [**"
                + track.getInfo().title
                + "**]("
                + track.getInfo().uri
                + ")");
      }
      builder.build().display(m);
    }

    @Override
    public void noMatches() {
      m.editMessage(
              FormatUtil.filter(
                  event.getClient().getWarning()
                      + " No results found for `"
                      + event.getArgs()
                      + "`."))
          .queue();
    }

    @Override
    public void loadFailed(FriendlyException throwable) {
      if (throwable.severity == Severity.COMMON)
        m.editMessage(event.getClient().getError() + " Error loading: " + throwable.getMessage())
            .queue();
      else m.editMessage(event.getClient().getError() + " Error loading track.").queue();
    }
  }
}
