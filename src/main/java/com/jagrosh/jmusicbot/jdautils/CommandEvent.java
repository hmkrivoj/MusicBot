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
import java.util.List;
import java.util.function.Consumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.internal.utils.Checks;

/**
 * A wrapper class for a {@link net.dv8tion.jda.api.events.message.MessageReceivedEvent
 * MessageReceivedEvent}, {@link com.jagrosh.jmusicbot.jdautils.CommandClient CommandClient}, and
 * String user arguments compatible with all {@link com.jagrosh.jmusicbot.jdautils.Command
 * Command}s.
 *
 * <p>From here, developers can invoke several useful and specialized methods to assist in Command
 * function and development. There are also "extension" methods for all methods found in
 * MessageReceivedEvent.
 *
 * <p>Methods with "reply" in their name can be used to instantly send a {@link
 * net.dv8tion.jda.api.entities.Message Message} response to the {@link
 * net.dv8tion.jda.api.entities.MessageChannel MessageChannel} the MessageReceivedEvent was in. <br>
 * All {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending a response
 * using these methods automatically {@link net.dv8tion.jda.api.requests.RestAction#queue()
 * RestAction#queue()}, and no further developer input is required.
 *
 * @author John Grosh (jagrosh)
 */
public class CommandEvent {
  public static final int MAX_MESSAGES = 2;

  private final MessageReceivedEvent event;
  private String args;
  private final CommandClient client;

  /**
   * Constructor for a CommandEvent.
   *
   * <p><b>You should not call this!</b> <br>
   * It is a generated wrapper for a {@link net.dv8tion.jda.api.events.message.MessageReceivedEvent
   * MessageReceivedEvent}.
   *
   * @param event The initial MessageReceivedEvent
   * @param args The String arguments after the command call
   * @param client The {@link com.jagrosh.jmusicbot.jdautils.CommandClient CommandClient}
   */
  public CommandEvent(MessageReceivedEvent event, String args, CommandClient client) {
    this.event = event;
    this.args = args == null ? "" : args;
    this.client = client;
  }

  /**
   * Returns the user's String arguments for the command. <br>
   * If no arguments have been supplied, then this will return an empty String.
   *
   * @return Never-null arguments that a user has supplied to a command
   */
  public String getArgs() {
    return args;
  }

  void setArgs(String args) {
    this.args = args;
  }

  /**
   * Returns the {@link com.jagrosh.jmusicbot.jdautils.CommandClient CommandClient} that initiated
   * this CommandEvent.
   *
   * @return The initiating CommandClient
   */
  public CommandClient getClient() {
    return client;
  }

  /**
   * Links a {@link net.dv8tion.jda.api.entities.Message Message} with the calling Message contained
   * by this CommandEvent.
   *
   * <p>This method is exposed for those who wish to use linked deletion but may require usage of
   * {@link net.dv8tion.jda.api.entities.MessageChannel#sendMessage(Message)
   * MessageChannel#sendMessage()} or for other reasons cannot use the standard {@code reply()}
   * methods.
   *
   * <p>If the Message provided is <b>not</b> from the bot (IE: {@link
   * net.dv8tion.jda.api.entities.SelfUser SelfUser}), an {@link java.lang.IllegalArgumentException
   * IllegalArgumentException} will be thrown.
   *
   * @param message The Message to add, must be from the SelfUser while linked deletion is being
   *     used.
   * @throws java.lang.IllegalArgumentException If the Message provided is not from the bot.
   */
  public void linkId(Message message) {
    Checks.check(
        message.getAuthor().equals(getSelfUser()),
        "Attempted to link a Message who's author was not the bot!");
    ((CommandClientImpl) client).linkIds(event.getMessageIdLong(), message);
  }

  // functional calls

  /**
   * Replies with a String message.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()}.
   *
   * <p><b>NOTE:</b> This message can exceed the 2000 character cap, and will be sent in two split
   * Messages.
   *
   * @param message A String message to reply with
   */
  public void reply(String message) {
    sendMessage(event.getChannel(), message);
  }

  /**
   * Replies with a String message and then queues a {@link java.util.function.Consumer}.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()} with the provided Consumer
   * as it's success callback.
   *
   * <p><b>NOTE:</b> This message can exceed the 2000 character cap, and will be sent in two split
   * Messages. <br>
   * The Consumer will be applied to the last message sent if this occurs.
   *
   * @param message A String message to reply with
   * @param success The Consumer to queue after sending the Message is sent.
   */
  public void reply(String message, Consumer<Message> success) {
    sendMessage(event.getChannel(), message, success);
  }

  /**
   * Replies with a String message and then queues a {@link java.util.function.Consumer}.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()} with the first Consumer as
   * it's success callback and the second Consumer as the failure callback.
   *
   * <p><b>NOTE:</b> This message can exceed the 2000 character cap, and will be sent in two split
   * Messages. <br>
   * Either Consumer will be applied to the last message sent if this occurs.
   *
   * @param message A String message to reply with
   * @param success The Consumer to queue after sending the Message is sent.
   * @param failure The Consumer to run if an error occurs when sending the Message.
   */
  public void reply(String message, Consumer<Message> success, Consumer<Throwable> failure) {
    sendMessage(event.getChannel(), message, success, failure);
  }

  /**
   * Replies with a {@link net.dv8tion.jda.api.entities.Message Message}.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()}.
   *
   * @param message The Message to reply with
   */
  public void reply(Message message) {
    event
        .getChannel()
        .sendMessage(message)
        .queue(
            m -> {
              if (event.isFromType(ChannelType.TEXT)) linkId(m);
            });
  }

  /**
   * Replies with a {@link net.dv8tion.jda.api.entities.Message Message} and then queues a {@link
   * java.util.function.Consumer}.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#success()} with the provided
   * Consumer as it's success callback.
   *
   * @param message The Message to reply with
   * @param success The Consumer to success after sending the Message is sent.
   */
  public void reply(Message message, Consumer<Message> success) {
    event
        .getChannel()
        .sendMessage(message)
        .queue(
            m -> {
              if (event.isFromType(ChannelType.TEXT)) linkId(m);
              success.accept(m);
            });
  }

  /**
   * Replies with a String message sent to the calling {@link net.dv8tion.jda.api.entities.User
   * User}'s {@link net.dv8tion.jda.api.entities.PrivateChannel PrivateChannel}.
   *
   * <p>If the User to be Direct Messaged does not already have a PrivateChannel open to send
   * messages to, this method will automatically open one.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()}.
   *
   * <p><b>NOTE:</b> This alternate String message can exceed the 2000 character cap, and will be
   * sent in two split Messages.
   *
   * @param message A String message to reply with
   */
  public void replyInDm(String message) {
    if (event.isFromType(ChannelType.PRIVATE)) reply(message);
    else {
      event.getAuthor().openPrivateChannel().queue(pc -> sendMessage(pc, message));
    }
  }

  /**
   * Replies with a String message sent to the calling {@link net.dv8tion.jda.api.entities.User
   * User}'s {@link net.dv8tion.jda.api.entities.PrivateChannel PrivateChannel}.
   *
   * <p>If the User to be Direct Messaged does not already have a PrivateChannel open to send
   * messages to, this method will automatically open one.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()} with the first Consumer as
   * it's success callback and the second Consumer as the failure callback.
   *
   * <p><b>NOTE:</b> This alternate String message can exceed the 2000 character cap, and will be
   * sent in two split Messages.
   *
   * @param message A String message to reply with
   * @param success The Consumer to queue after sending the Message is sent.
   * @param failure The Consumer to run if an error occurs when sending the Message.
   */
  public void replyInDm(String message, Consumer<Message> success, Consumer<Throwable> failure) {
    if (event.isFromType(ChannelType.PRIVATE)) reply(message, success, failure);
    else {
      event
          .getAuthor()
          .openPrivateChannel()
          .queue(pc -> sendMessage(pc, message, success, failure), failure);
    }
  }

  /**
   * Replies with a String message, and a prefixed success emoji.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()}.
   *
   * <p><b>NOTE:</b> This message can exceed the 2000 character cap, and will be sent in two split
   * Messages.
   *
   * @param message A String message to reply with
   */
  public void replySuccess(String message) {
    reply(client.getSuccess() + " " + message);
  }

  /**
   * Replies with a String message, and a prefixed warning emoji.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()}.
   *
   * <p><b>NOTE:</b> This message can exceed the 2000 character cap, and will be sent in two split
   * Messages.
   *
   * @param message A String message to reply with
   */
  public void replyWarning(String message) {
    reply(client.getWarning() + " " + message);
  }

  /**
   * Replies with a String message and a prefixed error emoji.
   *
   * <p>The {@link net.dv8tion.jda.api.requests.RestAction RestAction} returned by sending the
   * response as a {@link net.dv8tion.jda.api.entities.Message Message} automatically does {@link
   * net.dv8tion.jda.api.requests.RestAction#queue() RestAction#queue()}.
   *
   * <p><b>NOTE:</b> This message can exceed the 2000 character cap, and will be sent in two split
   * Messages.
   *
   * @param message A String message to reply with
   */
  public void replyError(String message) {
    reply(client.getError() + " " + message);
  }

  /**
   * Adds a success reaction to the calling {@link net.dv8tion.jda.api.entities.Message Message}.
   */
  public void reactSuccess() {
    react(client.getSuccess());
  }

  // private methods

  private void react(String reaction) {
    if (reaction == null || reaction.isEmpty()) return;
    try {
      event.getMessage().addReaction(reaction.replaceAll("<a?:(.+):(\\d+)>", "$1:$2")).queue();
    } catch (PermissionException ignored) {
      // do nothing
    }
  }

  private void sendMessage(MessageChannel chan, String message) {
    List<String> messages = splitMessage(message);
    for (int i = 0; i < MAX_MESSAGES && i < messages.size(); i++) {
      chan.sendMessage(messages.get(i))
          .queue(
              m -> {
                if (event.isFromType(ChannelType.TEXT)) linkId(m);
              });
    }
  }

  private void sendMessage(MessageChannel chan, String message, Consumer<Message> success) {
    List<String> messages = splitMessage(message);
    for (int i = 0; i < MAX_MESSAGES && i < messages.size(); i++) {
      if (i + 1 == MAX_MESSAGES || i + 1 == messages.size()) {
        chan.sendMessage(messages.get(i))
            .queue(
                m -> {
                  if (event.isFromType(ChannelType.TEXT)) linkId(m);
                  success.accept(m);
                });
      } else {
        chan.sendMessage(messages.get(i))
            .queue(
                m -> {
                  if (event.isFromType(ChannelType.TEXT)) linkId(m);
                });
      }
    }
  }

  private void sendMessage(
      MessageChannel chan, String message, Consumer<Message> success, Consumer<Throwable> failure) {
    List<String> messages = splitMessage(message);
    for (int i = 0; i < MAX_MESSAGES && i < messages.size(); i++) {
      if (i + 1 == MAX_MESSAGES || i + 1 == messages.size()) {
        chan.sendMessage(messages.get(i))
            .queue(
                m -> {
                  if (event.isFromType(ChannelType.TEXT)) linkId(m);
                  success.accept(m);
                },
                failure);
      } else {
        chan.sendMessage(messages.get(i))
            .queue(
                m -> {
                  if (event.isFromType(ChannelType.TEXT)) linkId(m);
                });
      }
    }
  }

  /**
   * Splits a String into one or more Strings who's length does not exceed 2000 characters. <br>
   * Also nullifies usages of {@code @here} and {@code @everyone} so that they do not mention
   * anyone. <br>
   * Useful for splitting long messages so that they can be sent in more than one {@link
   * net.dv8tion.jda.api.entities.Message Message} at maximum potential length.
   *
   * @param stringtoSend The String to split and send
   * @return An {@link java.util.ArrayList ArrayList} containing one or more Strings, with nullified
   *     occurrences of {@code @here} and {@code @everyone}, and that do not exceed 2000 characters
   *     in length
   */
  public static List<String> splitMessage(String stringtoSend) {
    ArrayList<String> msgs = new ArrayList<>();
    if (stringtoSend != null) {
      stringtoSend =
          stringtoSend.replace("@everyone", "@\u0435veryone").replace("@here", "@h\u0435re").trim();
      while (stringtoSend.length() > 2000) {
        int leeway = 2000 - (stringtoSend.length() % 2000);
        int index = stringtoSend.lastIndexOf("\n", 2000);
        if (index < leeway) index = stringtoSend.lastIndexOf(" ", 2000);
        if (index < leeway) index = 2000;
        String temp = stringtoSend.substring(0, index).trim();
        if (!temp.equals("")) msgs.add(temp);
        stringtoSend = stringtoSend.substring(index).trim();
      }
      if (!stringtoSend.equals("")) msgs.add(stringtoSend);
    }
    return msgs;
  }

  // custom shortcuts

  /**
   * Gets a {@link net.dv8tion.jda.api.entities.SelfUser SelfUser} representing the bot. <br>
   * This is the same as invoking {@code event.getJDA().getSelfUser()}.
   *
   * @return A User representing the bot
   */
  public SelfUser getSelfUser() {
    return event.getJDA().getSelfUser();
  }

  /**
   * Gets a {@link net.dv8tion.jda.api.entities.Member Member} representing the bot, or null if the
   * event does not take place on a {@link net.dv8tion.jda.api.entities.Guild Guild}. <br>
   * This is the same as invoking {@code event.getGuild().getSelfMember()}.
   *
   * @return A possibly-null Member representing the bot
   */
  public Member getSelfMember() {
    return event.getGuild() == null ? null : event.getGuild().getSelfMember();
  }

  /**
   * Tests whether or not the {@link net.dv8tion.jda.api.entities.User User} who triggered this
   * event is an owner of the bot.
   *
   * @return {@code true} if the User is the Owner, else {@code false}
   */
  public boolean isOwner() {
    if (event.getAuthor().getId().equals(this.getClient().getOwnerId())) return true;
    if (this.getClient().getCoOwnerIds() == null) return false;
    for (String id : this.getClient().getCoOwnerIds())
      if (id.equals(event.getAuthor().getId())) return true;
    return false;
  }

  // shortcuts

  /**
   * Gets the {@link net.dv8tion.jda.api.entities.User User} who triggered this CommandEvent.
   *
   * @return The User who triggered this CommandEvent
   */
  public User getAuthor() {
    return event.getAuthor();
  }

  /**
   * Gets the {@link net.dv8tion.jda.api.entities.MessageChannel MessageChannel} that the
   * CommandEvent was triggered on.
   *
   * @return The MessageChannel that the CommandEvent was triggered on
   */
  public MessageChannel getChannel() {
    return event.getChannel();
  }

  /**
   * Gets the {@link net.dv8tion.jda.api.entities.ChannelType ChannelType} of the {@link
   * net.dv8tion.jda.api.entities.MessageChannel MessageChannel} that the CommandEvent was triggered
   * on.
   *
   * @return The ChannelType of the MessageChannel that this CommandEvent was triggered on
   */
  public ChannelType getChannelType() {
    return event.getChannelType();
  }

  /**
   * Gets the {@link net.dv8tion.jda.api.entities.Guild Guild} that this CommandEvent was triggered
   * on.
   *
   * @return The Guild that this CommandEvent was triggered on
   */
  public Guild getGuild() {
    return event.getGuild();
  }

  /**
   * Gets the instance of {@link net.dv8tion.jda.api.JDA JDA} that this CommandEvent was caught by.
   *
   * @return The instance of JDA that this CommandEvent was caught by
   */
  public JDA getJDA() {
    return event.getJDA();
  }

  /**
   * Gets the {@link net.dv8tion.jda.api.entities.Member Member} that triggered this CommandEvent.
   *
   * @return The Member that triggered this CommandEvent
   */
  public Member getMember() {
    return event.getMember();
  }

  /**
   * Gets the {@link net.dv8tion.jda.api.entities.Message Message} responsible for triggering this
   * CommandEvent.
   *
   * @return The Message responsible for the CommandEvent
   */
  public Message getMessage() {
    return event.getMessage();
  }

  /**
   * Gets the {@link net.dv8tion.jda.api.entities.TextChannel TextChannel} that this CommandEvent
   * may have taken place on, or {@code null} if it didn't happen on a TextChannel.
   *
   * @return The TextChannel this CommandEvent may have taken place on, or null if it did not happen
   *     on a TextChannel.
   */
  public TextChannel getTextChannel() {
    return event.getTextChannel();
  }

  /**
   * Compares a provided {@link net.dv8tion.jda.api.entities.ChannelType ChannelType} with the one
   * this CommandEvent occurred on, returning {@code true} if they are the same ChannelType.
   *
   * @param channelType The ChannelType to compare
   * @return {@code true} if the CommandEvent originated from a {@link
   *     net.dv8tion.jda.api.entities.MessageChannel} of the provided ChannelType, otherwise {@code
   *     false}.
   */
  public boolean isFromType(ChannelType channelType) {
    return event.isFromType(channelType);
  }
}
