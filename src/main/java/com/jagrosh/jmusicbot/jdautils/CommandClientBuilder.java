/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.jdautils;

import com.jagrosh.jmusicbot.jdautils.impl.CommandClientImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

/**
 * A simple builder used to create a {@link com.jagrosh.jmusicbot.jdautils.impl.CommandClientImpl
 * CommandClientImpl}.
 *
 * <p>Once built, add the {@link com.jagrosh.jmusicbot.jdautils.CommandClient CommandClient} as an
 * EventListener to {@link net.dv8tion.jda.api.JDA JDA} and it will automatically handle commands
 * with ease!
 *
 * @author John Grosh (jagrosh)
 */
public class CommandClientBuilder {
  private Activity activity = Activity.playing("default");
  private OnlineStatus status = OnlineStatus.ONLINE;
  private String ownerId;
  private String[] coOwnerIds;
  private String prefix;
  private String altprefix;
  private String serverInvite;
  private String success;
  private String warning;
  private String error;
  private final LinkedList<Command> commands = new LinkedList<>();
  private CommandListener listener;
  private boolean useHelp = true;
  private boolean shutdownAutomatically = true;
  private Consumer<CommandEvent> helpConsumer;
  private String helpWord;
  private ScheduledExecutorService executor;
  private int linkedCacheSize = 0;
  private GuildSettingsManager manager = null;

  /**
   * Builds a {@link com.jagrosh.jmusicbot.jdautils.impl.CommandClientImpl CommandClientImpl} with
   * the provided settings. <br>
   * Once built, only the {@link com.jagrosh.jmusicbot.jdautils.CommandListener CommandListener},
   * and {@link com.jagrosh.jmusicbot.jdautils.Command Command}s can be changed.
   *
   * @return The CommandClient built.
   */
  public CommandClient build() {
    CommandClient client =
        new CommandClientImpl(
            ownerId,
            coOwnerIds,
            prefix,
            altprefix,
            activity,
            status,
            serverInvite,
            success,
            warning,
            error,
            new ArrayList<>(commands),
            useHelp,
            shutdownAutomatically,
            helpConsumer,
            helpWord,
            executor,
            linkedCacheSize,
            manager);
    if (listener != null) {
      client.setListener(listener);
    }
    return client;
  }

  /**
   * Sets the owner for the bot. <br>
   * Make sure to verify that the ID provided is ISnowflake compatible when setting this. If it is
   * not, this will warn the developer.
   *
   * @param ownerId The ID of the owner.
   * @return This builder
   */
  public CommandClientBuilder setOwnerId(String ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  /**
   * Sets the bot's prefix. <br>
   * If set null, empty, or not set at all, the bot will use a mention {@literal @Botname} as a
   * prefix.
   *
   * @param prefix The prefix for the bot to use
   * @return This builder
   */
  public CommandClientBuilder setPrefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * Sets the bot's alternative prefix. <br>
   * If set null, the bot will only use its primary prefix prefix.
   *
   * @param prefix The alternative prefix for the bot to use
   * @return This builder
   */
  public CommandClientBuilder setAlternativePrefix(String prefix) {
    this.altprefix = prefix;
    return this;
  }

  /**
   * Sets the word used to trigger the command list. <br>
   * Setting this to {@code null} or not setting this at all will set the help word to {@code
   * "help"}.
   *
   * @param helpWord The word to trigger the help command
   * @return This builder
   */
  public CommandClientBuilder setHelpWord(String helpWord) {
    this.helpWord = helpWord;
    return this;
  }

  /**
   * Sets the emojis for success, warning, and failure.
   *
   * @param success Emoji for success
   * @param warning Emoji for warning
   * @param error Emoji for failure
   * @return This builder
   */
  public CommandClientBuilder setEmojis(String success, String warning, String error) {
    this.success = success;
    this.warning = warning;
    this.error = error;
    return this;
  }

  /**
   * Sets the {@link net.dv8tion.jda.api.entities.Activity Game} to use when the bot is ready. <br>
   * Can be set to {@code null} for no activity.
   *
   * @param activity The Game to use when the bot is ready
   * @return This builder
   */
  public CommandClientBuilder setActivity(Activity activity) {
    this.activity = activity;
    return this;
  }

  /**
   * Sets the {@link net.dv8tion.jda.api.entities.Activity Game} the bot will use as the default:
   * 'Playing <b>Type [prefix]help</b>'
   *
   * @return This builder
   */
  public CommandClientBuilder useDefaultGame() {
    this.activity = Activity.playing("default");
    return this;
  }

  /**
   * Sets the {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} the bot will use once Ready This
   * defaults to ONLINE
   *
   * @param status The status to set
   * @return This builder
   */
  public CommandClientBuilder setStatus(OnlineStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Adds a {@link com.jagrosh.jmusicbot.jdautils.Command Command} and registers it to the {@link
   * com.jagrosh.jmusicbot.jdautils.impl.CommandClientImpl CommandClientImpl} for this session.
   *
   * @param command The command to add
   * @return This builder
   */
  public CommandClientBuilder addCommand(Command command) {
    commands.add(command);
    return this;
  }

  /**
   * Adds and registers multiple {@link com.jagrosh.jmusicbot.jdautils.Command Command}s to the
   * {@link com.jagrosh.jmusicbot.jdautils.impl.CommandClientImpl CommandClientImpl} for this
   * session. <br>
   * This is the same as calling {@link
   * com.jagrosh.jmusicbot.jdautils.CommandClientBuilder#addCommand(Command)} multiple times.
   *
   * @param commands The Commands to add
   * @return This builder
   */
  public CommandClientBuilder addCommands(Collection<Command> commands) {
    for (Command command : commands) this.addCommand(command);
    return this;
  }

  /**
   * Sets the internal size of the client's {@link
   * com.jagrosh.jmusicbot.jdautils.utils.FixedSizeCache FixedSizeCache} used for caching and
   * pairing the bot's response {@link net.dv8tion.jda.api.entities.Message Message}s with the
   * calling Message's ID.
   *
   * <p>Higher cache size means that decay of cache contents will most likely occur later, allowing
   * the deletion of responses when the call is deleted to last for a longer duration. However this
   * also means larger memory usage.
   *
   * <p>Setting {@code 0} or negative will cause the client to not use linked caching <b>at all</b>.
   *
   * @param linkedCacheSize The maximum number of paired responses that can be cached, or {@code <1}
   *     if the built {@link com.jagrosh.jmusicbot.jdautils.CommandClient CommandClient} will not
   *     use linked caching.
   * @return This builder
   */
  public CommandClientBuilder setLinkedCacheSize(int linkedCacheSize) {
    this.linkedCacheSize = linkedCacheSize;
    return this;
  }

  /**
   * Sets the {@link com.jagrosh.jmusicbot.jdautils.GuildSettingsManager GuildSettingsManager} for
   * the CommandClientImpl built using this builder.
   *
   * @param manager The GuildSettingsManager to set.
   * @return This builder
   */
  public CommandClientBuilder setGuildSettingsManager(GuildSettingsManager manager) {
    this.manager = manager;
    return this;
  }
}
