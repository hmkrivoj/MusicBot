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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.EventListener;

/**
 * A Bot Client interface implemented on objects used to hold bot data.
 *
 * <p>This is implemented in {@link com.jagrosh.jmusicbot.jdautils.impl.CommandClientImpl
 * CommandClientImpl} alongside implementation of {@link net.dv8tion.jda.api.hooks.EventListener
 * EventListener} to create a compounded "Client Listener" which catches specific kinds of events
 * thrown by JDA and processes them automatically to handle and execute {@link
 * com.jagrosh.jmusicbot.jdautils.Command Command}s.
 *
 * <p>Implementations also serve as a useful platforms, carrying reference info such as the bot's
 * {@linkplain #getOwnerId() Owner ID}, {@linkplain #getPrefix() prefix}, and a {@linkplain
 * #getServerInvite() support server invite}.
 *
 * <p>For the CommandClientImpl, once initialized, only the following can be modified:
 *
 * <ul>
 *   <li>{@link com.jagrosh.jmusicbot.jdautils.Command Command}s may be added or removed.
 *   <li>The {@link com.jagrosh.jmusicbot.jdautils.CommandListener CommandListener} may be set.
 * </ul>
 *
 * @author John Grosh (jagrosh)
 * @implNote While typically safe, there are a few ways to misuse the standard implementation of
 *     this interface: the CommandClientImpl. <br>
 *     Because of this the following should <b>ALWAYS</b> be followed to avoid such errors:
 *     <p><b>1)</b> Do not build and add more than one CommandClient to an instance JDA,
 *     <b>EVER</b>.
 *     <p><b>2)</b> Always create and add the CommandClientImpl to JDA <b>BEFORE</b> you build it,
 *     or there is a chance some minor errors will occur, <b>especially</b> if JDA has already fired
 *     a {@link net.dv8tion.jda.api.events.ReadyEvent ReadyEvent}.
 *     <p><b>3)</b> Do not provide anything other than a String representing a long (and furthermore
 *     a User ID) as an Owner ID or a CoOwner ID. This will generate errors, but not stop the
 *     creation of the CommandClientImpl which will cause several errors to occur very quickly after
 *     startup (except if you provide {@code null} for the Owner ID, that'll just flat out throw an
 *     {@link java.lang.IllegalArgumentException IllegalArgumentException}).
 *     <p><b>4)</b> Do not provide strings when using {@link
 *     com.jagrosh.jmusicbot.jdautils.CommandClientBuilder#setEmojis(String, String, String)
 *     CommandClientBuilder#setEmojis(String, String, String)} that are not unicode emojis or that
 *     do not match the custom emote format specified in {@link
 *     net.dv8tion.jda.api.entities.Emote#getAsMention() Emote#getAsMention()} (IE: {@code
 *     <:EmoteName:EmoteID>}).
 *     <p><b>5)</b> Avoid using {@link
 *     com.jagrosh.jmusicbot.jdautils.impl.CommandClientImpl#linkIds(long,
 *     net.dv8tion.jda.api.entities.Message)}. This will create errors and has no real purpose
 *     outside of it's current usage.
 */
public interface CommandClient extends EventListener {
  /**
   * Gets the Client's prefix.
   *
   * @return A possibly-null prefix
   */
  String getPrefix();

  /**
   * Returns the visual representation of the bot's prefix.
   *
   * <p>This is the same as {@link com.jagrosh.jmusicbot.jdautils.CommandClient#getPrefix() } unless
   * the prefix is the default, in which case it appears as {@literal @Botname}.
   *
   * @return A never-null prefix
   */
  String getTextualPrefix();

  /**
   * Sets the {@link com.jagrosh.jmusicbot.jdautils.CommandListener CommandListener} to catch
   * command-related events thrown by this {@link com.jagrosh.jmusicbot.jdautils.CommandClient
   * CommandClient}.
   *
   * @param listener The CommandListener
   */
  void setListener(CommandListener listener);

  /**
   * Returns the current {@link com.jagrosh.jmusicbot.jdautils.CommandListener CommandListener}.
   *
   * @return A possibly-null CommandListener
   */
  CommandListener getListener();

  /**
   * Gets the remaining number of seconds on the specified cooldown.
   *
   * @param name The cooldown name
   * @return The number of seconds remaining
   */
  int getRemainingCooldown(String name);

  /**
   * Applies the specified cooldown with the provided name.
   *
   * @param name The cooldown name
   * @param seconds The time to make the cooldown last
   */
  void applyCooldown(String name, int seconds);

  /**
   * Gets the ID of the owner of this bot as a String.
   *
   * @return The String ID of the owner of the bot
   */
  String getOwnerId();

  /**
   * Gets the ID(s) of any CoOwners of this bot as a String Array.
   *
   * @return The String ID(s) of any CoOwners of this bot
   */
  String[] getCoOwnerIds();

  /**
   * Gets the success emoji.
   *
   * @return The success emoji
   */
  String getSuccess();

  /**
   * Gets the warning emoji.
   *
   * @return The warning emoji
   */
  String getWarning();

  /**
   * Gets the error emoji.
   *
   * @return The error emoji
   */
  String getError();

  /**
   * Gets the word used to invoke a help DM.
   *
   * @return The help word
   */
  String getHelpWord();

  /**
   * Returns an Object of the type parameter that should contain settings relating to the specified
   * {@link net.dv8tion.jda.api.entities.Guild Guild}.
   *
   * <p>The returning object for this is specified via provision of a {@link
   * com.jagrosh.jmusicbot.jdautils.GuildSettingsManager GuildSettingsManager} to {@link
   * com.jagrosh.jmusicbot.jdautils.CommandClientBuilder#setGuildSettingsManager(com.jagrosh.jmusicbot.jdautils.GuildSettingsManager)
   * CommandClientBuilder#setGuildSettingsManager(GuildSettingsManager)}, more specifically {@link
   * GuildSettingsManager#getSettings(net.dv8tion.jda.api.entities.Guild)
   * GuildSettingsManager#getSettings(Guild)}.
   *
   * @param  <S> The type of settings the GuildSettingsManager provides
   * @param guild The Guild to get Settings for
   * @return The settings object for the Guild, specified in {@link
   *     com.jagrosh.jmusicbot.jdautils.GuildSettingsManager#getSettings(Guild)
   *     GuildSettingsManager#getSettings(Guild)}, can be {@code null} if the implementation allows
   *     it.
   */
  <S> S getSettingsFor(Guild guild);
}
