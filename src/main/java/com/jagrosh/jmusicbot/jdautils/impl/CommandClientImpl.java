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
package com.jagrosh.jmusicbot.jdautils.impl;

import com.jagrosh.jmusicbot.jdautils.Command;
import com.jagrosh.jmusicbot.jdautils.CommandClient;
import com.jagrosh.jmusicbot.jdautils.CommandEvent;
import com.jagrosh.jmusicbot.jdautils.CommandListener;
import com.jagrosh.jmusicbot.jdautils.GuildSettingsManager;
import com.jagrosh.jmusicbot.jdautils.GuildSettingsProvider;
import com.jagrosh.jmusicbot.jdautils.utils.FixedSizeCache;
import com.jagrosh.jmusicbot.jdautils.utils.SafeIdUtil;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.internal.utils.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link com.jagrosh.jmusicbot.jdautils.CommandClient CommandClient} to be
 * used by a bot.
 *
 * <p>This is a listener usable with {@link net.dv8tion.jda.api.JDA JDA}, as it implements {@link
 * net.dv8tion.jda.api.hooks.EventListener EventListener} in order to catch and use different kinds
 * of {@link net.dv8tion.jda.api.events.Event Event}s. The primary usage of this is where the
 * CommandClient implementation takes {@link net.dv8tion.jda.api.events.message.MessageReceivedEvent
 * MessageReceivedEvent}s, and automatically processes arguments, and provide them to a {@link
 * com.jagrosh.jmusicbot.jdautils.Command Command} for running and execution.
 *
 * @author John Grosh (jagrosh)
 */
public class CommandClientImpl implements CommandClient {
  private static final Logger LOG = LoggerFactory.getLogger(CommandClientImpl.class);
  private static final String DEFAULT_PREFIX = "@mention";

  private final Activity activity;
  private final OnlineStatus status;
  private final String ownerId;
  private final String[] coOwnerIds;
  private final String prefix;
  private final String altprefix;
  private final HashMap<String, Integer> commandIndex;
  private final ArrayList<Command> commands;
  private final String success;
  private final String warning;
  private final String error;
  private final HashMap<String, OffsetDateTime> cooldowns;
  private final HashMap<String, Integer> uses;
  private final FixedSizeCache<Long, Set<Message>> linkMap;
  private final boolean useHelp;
  private final boolean shutdownAutomatically;
  private final Consumer<CommandEvent> helpConsumer;
  private final String helpWord;
  private final ScheduledExecutorService executor;
  private final GuildSettingsManager manager;

  private String textPrefix;
  private CommandListener listener = null;

  public CommandClientImpl(
      String ownerId,
      String[] coOwnerIds,
      String prefix,
      String altprefix,
      Activity activity,
      OnlineStatus status,
      String serverInvite,
      String success,
      String warning,
      String error,
      List<Command> commands,
      boolean useHelp,
      boolean shutdownAutomatically,
      Consumer<CommandEvent> helpConsumer,
      String helpWord,
      ScheduledExecutorService executor,
      int linkedCacheSize,
      GuildSettingsManager manager) {
    Checks.check(
        ownerId != null,
        "Owner ID was set null or not set! Please provide an User ID to register as the owner!");

    if (!SafeIdUtil.checkId(ownerId)) {
      LOG.warn(
          "The provided Owner ID ({}) was found unsafe! Make sure ID is a non-negative long!",
          ownerId);
    }

    if (coOwnerIds != null) {
      for (String coOwnerId : coOwnerIds) {
        if (!SafeIdUtil.checkId(coOwnerId)) {
          LOG.warn(
              "The provided CoOwner ID ({}) was found unsafe! Make sure ID is a non-negative long!",
              coOwnerId);
        }
      }
    }

    this.ownerId = ownerId;
    this.coOwnerIds = coOwnerIds;
    this.prefix = prefix == null || prefix.isEmpty() ? DEFAULT_PREFIX : prefix;
    this.altprefix = altprefix == null || altprefix.isEmpty() ? null : altprefix;
    this.textPrefix = prefix;
    this.activity = activity;
    this.status = status;
    this.success = success == null ? "" : success;
    this.warning = warning == null ? "" : warning;
    this.error = error == null ? "" : error;
    this.commandIndex = new HashMap<>();
    this.commands = new ArrayList<>();
    this.cooldowns = new HashMap<>();
    this.uses = new HashMap<>();
    this.linkMap = linkedCacheSize > 0 ? new FixedSizeCache<>(linkedCacheSize) : null;
    this.useHelp = useHelp;
    this.shutdownAutomatically = shutdownAutomatically;
    this.helpWord = helpWord == null ? "help" : helpWord;
    this.executor = executor == null ? Executors.newSingleThreadScheduledExecutor() : executor;
    this.manager = manager;
    this.helpConsumer =
        helpConsumer == null
            ? event -> {
              StringBuilder builder =
                  new StringBuilder("**" + event.getSelfUser().getName() + "** commands:\n");
              Command.Category category = null;
              for (Command command : commands) {
                if (!command.isHidden() && (!command.isOwnerCommand() || event.isOwner())) {
                  if (!Objects.equals(category, command.getCategory())) {
                    category = command.getCategory();
                    builder
                        .append("\n\n  __")
                        .append(category == null ? "No Category" : category.getName())
                        .append("__:\n");
                  }
                  builder
                      .append("\n`")
                      .append(textPrefix)
                      .append(prefix == null ? " " : "")
                      .append(command.getName())
                      .append(
                          command.getArguments() == null ? "`" : " " + command.getArguments() + "`")
                      .append(" - ")
                      .append(command.getHelp());
                }
              }
              User owner = event.getJDA().getUserById(ownerId);
              if (owner != null) {
                builder
                    .append("\n\nFor additional help, contact **")
                    .append(owner.getName())
                    .append("**#")
                    .append(owner.getDiscriminator());
                if (serverInvite != null) builder.append(" or join ").append(serverInvite);
              }
              event.replyInDm(
                  builder.toString(),
                  unused -> {
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess();
                  },
                  t ->
                      event.replyWarning(
                          "Help cannot be sent because you are blocking Direct Messages."));
            }
            : helpConsumer;

    // Load commands
    for (Command command : commands) {
      addCommand(command);
    }
  }

  @Override
  public void setListener(CommandListener listener) {
    this.listener = listener;
  }

  @Override
  public CommandListener getListener() {
    return listener;
  }

  @Override
  public int getRemainingCooldown(String name) {
    if (cooldowns.containsKey(name)) {
      int time =
          (int)
              Math.ceil(OffsetDateTime.now().until(cooldowns.get(name), ChronoUnit.MILLIS) / 1000D);
      if (time <= 0) {
        cooldowns.remove(name);
        return 0;
      }
      return time;
    }
    return 0;
  }

  @Override
  public void applyCooldown(String name, int seconds) {
    cooldowns.put(name, OffsetDateTime.now().plusSeconds(seconds));
  }

  private void addCommand(Command command) {
    addCommand(command, commands.size());
  }

  private void addCommand(Command command, int index) {
    if (index > commands.size() || index < 0)
      throw new ArrayIndexOutOfBoundsException(
          "Index specified is invalid: [" + index + "/" + commands.size() + "]");
    synchronized (commandIndex) {
      String name = command.getName().toLowerCase();
      // check for collision
      if (commandIndex.containsKey(name))
        throw new IllegalArgumentException(
            "Command added has a name or alias that has already been indexed: \"" + name + "\"!");
      for (String alias : command.getAliases()) {
        if (commandIndex.containsKey(alias.toLowerCase()))
          throw new IllegalArgumentException(
              "Command added has a name or alias that has already been indexed: \""
                  + alias
                  + "\"!");
      }
      // shift if not append
      if (index < commands.size()) {
        commandIndex.entrySet().stream()
            .filter(entry -> entry.getValue() >= index)
            .forEach(entry -> commandIndex.put(entry.getKey(), entry.getValue() + 1));
      }
      // add
      commandIndex.put(name, index);
      for (String alias : command.getAliases()) commandIndex.put(alias.toLowerCase(), index);
    }
    commands.add(index, command);
  }

  @Override
  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public String[] getCoOwnerIds() {
    return coOwnerIds;
  }

  @Override
  public String getSuccess() {
    return success;
  }

  @Override
  public String getWarning() {
    return warning;
  }

  @Override
  public String getError() {
    return error;
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

  @Override
  public String getTextualPrefix() {
    return textPrefix;
  }

  @Override
  public String getHelpWord() {
    return helpWord;
  }

  private boolean usesLinkedDeletion() {
    return linkMap != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S> S getSettingsFor(Guild guild) {
    if (manager == null) return null;
    return (S) manager.getSettings(guild);
  }

  @SuppressWarnings("unchecked")
  private <M extends GuildSettingsManager> M getSettingsManager() {
    return (M) manager;
  }

  private void shutdown() {
    GuildSettingsManager<?> manager = getSettingsManager();
    if (manager != null) manager.shutdown();
    executor.shutdown();
  }

  @Override
  public void onEvent(GenericEvent event) {
    if (event instanceof MessageReceivedEvent messageReceivedEvent)
      onMessageReceived(messageReceivedEvent);
    else if (event instanceof GuildMessageDeleteEvent guildMessageDeleteEvent
        && usesLinkedDeletion()) onMessageDelete(guildMessageDeleteEvent);
    else if (event instanceof GuildJoinEvent) {
      // do nothing
    } else if (event instanceof GuildLeaveEvent) {
      // do nothing
    } else if (event instanceof ReadyEvent readyEvent) {
      onReady(readyEvent);
    } else if (event instanceof ShutdownEvent && shutdownAutomatically) {
      shutdown();
    }
  }

  private void onReady(ReadyEvent event) {
    if (!event.getJDA().getSelfUser().isBot()) {
      LOG.error("JDA-Utilities does not support CLIENT accounts.");
      event.getJDA().shutdown();
      return;
    }
    textPrefix =
        prefix.equals(DEFAULT_PREFIX) ? "@" + event.getJDA().getSelfUser().getName() + " " : prefix;
    event
        .getJDA()
        .getPresence()
        .setPresence(
            status == null ? OnlineStatus.ONLINE : status,
            activity == null
                ? null
                : "default".equals(activity.getName())
                    ? Activity.playing("Type " + textPrefix + helpWord)
                    : activity);

    // Start SettingsManager if necessary
    GuildSettingsManager<?> m = getSettingsManager();
    if (m != null) m.init();
  }

  private void onMessageReceived(MessageReceivedEvent event) {
    // Return if it's a bot
    if (event.getAuthor().isBot()) return;

    String[] parts = null;
    String rawContent = event.getMessage().getContentRaw();

    GuildSettingsProvider settings =
        event.isFromType(ChannelType.TEXT) ? provideSettings(event.getGuild()) : null;

    // Check for prefix or alternate prefix (@mention cases)
    if ((prefix.equals(DEFAULT_PREFIX) || (altprefix != null && altprefix.equals(DEFAULT_PREFIX)))
        && (rawContent.startsWith("<@" + event.getJDA().getSelfUser().getId() + ">")
            || rawContent.startsWith("<@!" + event.getJDA().getSelfUser().getId() + ">"))) {
      parts = splitOnPrefixLength(rawContent, rawContent.indexOf(">") + 1);
    }
    // Check for prefix
    if (parts == null && rawContent.toLowerCase().startsWith(prefix.toLowerCase()))
      parts = splitOnPrefixLength(rawContent, prefix.length());
    // Check for alternate prefix
    if (parts == null
        && altprefix != null
        && rawContent.toLowerCase().startsWith(altprefix.toLowerCase()))
      parts = splitOnPrefixLength(rawContent, altprefix.length());
    // Check for guild specific prefixes
    if (parts == null && settings != null) {
      Collection<String> prefixes = settings.getPrefixes();
      if (prefixes != null) {
        for (String p : prefixes) {
          if (parts == null && rawContent.toLowerCase().startsWith(p.toLowerCase())) {
            parts = splitOnPrefixLength(rawContent, p.length());
          }
        }
      }
    }

    if (parts != null) // starts with valid prefix
    {
      if (useHelp && parts[0].equalsIgnoreCase(helpWord)) {
        CommandEvent cevent = new CommandEvent(event, parts[1] == null ? "" : parts[1], this);
        if (listener != null) listener.onCommand(cevent, null);
        helpConsumer.accept(cevent); // Fire help consumer
        if (listener != null) listener.onCompletedCommand(cevent, null);
        return; // Help Consumer is done
      } else if (event.isFromType(ChannelType.PRIVATE) || event.getTextChannel().canTalk()) {
        String name = parts[0];
        String args = parts[1] == null ? "" : parts[1];
        final Command command; // this will be null if it's not a command
        synchronized (commandIndex) {
          int i = commandIndex.getOrDefault(name.toLowerCase(), -1);
          command = i != -1 ? commands.get(i) : null;
        }

        if (command != null) {
          CommandEvent cevent = new CommandEvent(event, args, this);

          if (listener != null) listener.onCommand(cevent, command);
          uses.put(command.getName(), uses.getOrDefault(command.getName(), 0) + 1);
          command.run(cevent);
          return; // Command is done
        }
      }
    }

    if (listener != null) listener.onNonCommandMessage(event);
  }

  private void onMessageDelete(GuildMessageDeleteEvent event) {
    // We don't need to cover whether or not this client usesLinkedDeletion() because
    // that is checked in onEvent(Event) before this is even called.
    synchronized (linkMap) {
      if (linkMap.contains(event.getMessageIdLong())) {
        Set<Message> messages = linkMap.get(event.getMessageIdLong());
        if (messages.size() > 1
            && event
                .getGuild()
                .getSelfMember()
                .hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE))
          event.getChannel().deleteMessages(messages).queue(unused -> {}, ignored -> {});
        else if (!messages.isEmpty())
          messages.forEach(m -> m.delete().queue(unused -> {}, ignored -> {}));
      }
    }
  }

  private GuildSettingsProvider provideSettings(Guild guild) {
    Object settings = getSettingsFor(guild);
    if (settings instanceof GuildSettingsProvider guildSettingsProvider) {
      return guildSettingsProvider;
    }
    return null;
  }

  private static String[] splitOnPrefixLength(String rawContent, int length) {
    return Arrays.copyOf(rawContent.substring(length).trim().split("\\s+", 2), 2);
  }

  /**
   * <b>DO NOT USE THIS!</b>
   *
   * <p>This is a method necessary for linking a bot's response messages to their corresponding call
   * message ID. <br>
   * <b>Using this anywhere in your code can and will break your bot.</b>
   *
   * @param callId The ID of the call Message
   * @param message The Message to link to the ID
   */
  public void linkIds(long callId, Message message) {
    // We don't use linked deletion, so we don't do anything.
    if (!usesLinkedDeletion()) return;

    synchronized (linkMap) {
      Set<Message> stored = linkMap.get(callId);
      if (stored != null) stored.add(message);
      else {
        stored = new HashSet<>();
        stored.add(message);
        linkMap.add(callId, stored);
      }
    }
  }
}
