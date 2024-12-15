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
package com.jagrosh.jmusicbot.jdautils.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;

/**
 * A series of query based utilities for finding entities, either globally across all accessible
 * {@link net.dv8tion.jda.api.entities.Guild Guild}s, or locally to a specified Guild.
 *
 * <p>All methods use a similar priority hierarchy and return an immutable {@link java.util.List
 * List} based on the results. <br>
 * The hierarchy is as follows:
 *
 * <ul>
 *   <li>Special Cases: Specifics of these are described per individual method documentation. <br>
 *       Note that successful results from these are typically {@link
 *       java.util.Collections#singletonList(Object) a singleton list}.
 *   <li>Direct ID: Query is a number with 17 or more digits, resembling an {@link
 *       net.dv8tion.jda.api.entities.ISnowflake Snowflake} ID.
 *   <li>Exact Match: Query provided is an exact match (case sensitive and complete) to one or more
 *       entities.
 *   <li>Wrong Case: Query provided is a case-insensitive, but exact, match to the entirety of one
 *       or more entities.
 *   <li>Starting With: Query provided is an case-insensitive match to the beginning of one or more
 *       entities.
 *   <li>Contains: Query provided is a case-insensitive match to a part of one or more entities.
 * </ul>
 *
 * All queries return the highest List in this hierarchy that contains one or more entities, and
 * only of these kind of results (IE: the "exact" list will never contain any results from a
 * successful "starting with" match, unless by chance they could technically be the same result).
 *
 * <p><b>Shard Manager Usage</b> <br>
 * Methods that query an instance of {@link net.dv8tion.jda.api.JDA JDA} always have two
 * implementations:
 *
 * <ul>
 *   <li><b>Global:</b> Queries a {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} if
 *       one is available, or JDA if one is not.
 *   <li><b>Shard:</b> Always queries the provided instance, and never a ShardManager, even if one
 *       is available.
 * </ul>
 *
 * <p>Many of these utilities were inspired by and ported to JDA 3.X from <a
 * href="https://github.com/jagrosh/Spectra/blob/master/src/spectra/utils/FinderUtil.java">Spectra's
 * FinderUtil</a> originally written by <a href="https://github.com/jagrosh/">jagrosh</a> in 2.X.
 *
 * @since 1.3
 * @author Kaidan Gustave
 */
@SuppressWarnings("Duplicates")
public final class FinderUtil {
  public static final Pattern DISCORD_ID = Pattern.compile("\\d{17,20}"); // ID
  public static final Pattern FULL_USER_REF =
      Pattern.compile("(\\S.{0,30}\\S)\\s*#(\\d{4})"); // $1 -> username, $2 -> discriminator
  public static final Pattern USER_MENTION = Pattern.compile("<@!?(\\d{17,20})>"); // $1 -> ID
  public static final Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d{17,20})>"); // $1 -> ID
  public static final Pattern ROLE_MENTION = Pattern.compile("<@&(\\d{17,20})>"); // $1 -> ID

  /**
   * Queries a provided {@link net.dv8tion.jda.api.entities.Guild Guild} for {@link
   * net.dv8tion.jda.api.entities.Member Member}s.
   *
   * <p>The following special cases are applied in order of listing before the standard search is
   * done:
   *
   * <ul>
   *   <li>User Mention: Query provided matches an @user mention (more specifically {@literal
   *       <@userID> or <@!userID>}).
   *   <li>Full User Reference: Query provided matches a full Username#XXXX reference. <br>
   *       <b>NOTE:</b> this can return a list with more than one entity.
   * </ul>
   *
   * <p>Unlike {@link FinderUtil#findUsers(String, JDA) FinderUtil.findUsers(String, JDA)}, this
   * method queries based on two different names: user name and effective name (excluding special
   * cases in which it queries solely based on user name). <br>
   * Each standard check looks at the user name, then the member name, and if either one's criteria
   * is met the Member is added to the returned list. This is important to note, because the
   * returned list may contain exact matches for User's name as well as exact matches for a Member's
   * effective name, with nothing guaranteeing the returns will be exclusively containing matches
   * for one or the other. <br>
   * Information on effective name can be found in {@link
   * net.dv8tion.jda.api.entities.Member#getEffectiveName() Member#getEffectiveName()}.
   *
   * @param query The String query to search by
   * @param guild The Guild to search from
   * @return A possibly empty {@link java.util.List List} of Members found by the query from the
   *     provided Guild.
   */
  public static List<Member> findMembers(String query, Guild guild) {
    Matcher userMention = USER_MENTION.matcher(query);
    Matcher fullRefMatch = FULL_USER_REF.matcher(query);
    if (userMention.matches()) {
      Member member = guild.getMemberById(userMention.group(1));
      if (member != null) {
        return Collections.singletonList(member);
      }
    } else if (fullRefMatch.matches()) {
      String lowerName = fullRefMatch.group(1).toLowerCase();
      String discrim = fullRefMatch.group(2);
      List<Member> members =
          guild.getMemberCache().stream()
              .filter(
                  member ->
                      member.getUser().getName().toLowerCase().equals(lowerName)
                          && member.getUser().getDiscriminator().equals(discrim))
              .toList();
      if (!members.isEmpty()) {
        return members;
      }
    } else if (DISCORD_ID.matcher(query).matches()) {
      Member member = guild.getMemberById(query);
      if (member != null) {
        return Collections.singletonList(member);
      }
    }
    List<Member> exact = new ArrayList<>();
    List<Member> wrongcase = new ArrayList<>();
    List<Member> startswith = new ArrayList<>();
    List<Member> contains = new ArrayList<>();
    String lowerquery = query.toLowerCase();
    guild
        .getMemberCache()
        .forEach(
            member -> {
              String name = member.getUser().getName();
              String effName = member.getEffectiveName();
              if (name.equals(query) || effName.equals(query)) {
                exact.add(member);
              } else if ((name.equalsIgnoreCase(query) || effName.equalsIgnoreCase(query))
                  && exact.isEmpty()) {
                wrongcase.add(member);
              } else if ((name.toLowerCase().startsWith(lowerquery)
                      || effName.toLowerCase().startsWith(lowerquery))
                  && wrongcase.isEmpty()) {
                startswith.add(member);
              } else if ((name.toLowerCase().contains(lowerquery)
                      || effName.toLowerCase().contains(lowerquery))
                  && startswith.isEmpty()) {
                contains.add(member);
              }
            });
    if (!exact.isEmpty()) {
      return Collections.unmodifiableList(exact);
    }
    if (!wrongcase.isEmpty()) {
      return Collections.unmodifiableList(wrongcase);
    }
    if (!startswith.isEmpty()) {
      return Collections.unmodifiableList(startswith);
    }
    return Collections.unmodifiableList(contains);
  }

  /**
   * Queries a provided {@link net.dv8tion.jda.api.entities.Guild Guild} for {@link
   * net.dv8tion.jda.api.entities.TextChannel TextChannel}s.
   *
   * <p>The following special case is applied before the standard search is done:
   *
   * <ul>
   *   <li>Channel Mention: Query provided matches a #channel mention (more specifically {@literal
   *       <#channelID>})
   * </ul>
   *
   * @param query The String query to search by
   * @param guild The Guild to search from
   * @return A possibly-empty {@link java.util.List List} of TextChannels found by the query from
   *     the provided Guild.
   */
  public static List<TextChannel> findTextChannels(String query, Guild guild) {
    Matcher channelMention = CHANNEL_MENTION.matcher(query);
    if (channelMention.matches()) {
      TextChannel tc = guild.getTextChannelById(channelMention.group(1));
      if (tc != null) {
        return Collections.singletonList(tc);
      }
    } else if (DISCORD_ID.matcher(query).matches()) {
      TextChannel tc = guild.getTextChannelById(query);
      if (tc != null) {
        return Collections.singletonList(tc);
      }
    }

    return genericTextChannelSearch(query, guild.getTextChannelCache());
  }

  private static List<TextChannel> genericTextChannelSearch(
      String query, SnowflakeCacheView<TextChannel> cache) {
    List<TextChannel> exact = new ArrayList<>();
    List<TextChannel> wrongcase = new ArrayList<>();
    List<TextChannel> startswith = new ArrayList<>();
    List<TextChannel> contains = new ArrayList<>();
    String lowerquery = query.toLowerCase();
    cache.forEach(
        tc -> {
          String name = tc.getName();
          if (name.equals(query)) {
            exact.add(tc);
          } else if (name.equalsIgnoreCase(query) && exact.isEmpty()) {
            wrongcase.add(tc);
          } else if (name.toLowerCase().startsWith(lowerquery) && wrongcase.isEmpty()) {
            startswith.add(tc);
          } else if (name.toLowerCase().contains(lowerquery) && startswith.isEmpty()) {
            contains.add(tc);
          }
        });
    if (!exact.isEmpty()) {
      return Collections.unmodifiableList(exact);
    }
    if (!wrongcase.isEmpty()) {
      return Collections.unmodifiableList(wrongcase);
    }
    if (!startswith.isEmpty()) {
      return Collections.unmodifiableList(startswith);
    }
    return Collections.unmodifiableList(contains);
  }

  /**
   * Queries a provided {@link net.dv8tion.jda.api.entities.Guild Guild} for {@link
   * net.dv8tion.jda.api.entities.VoiceChannel VoiceChannel}s.
   *
   * <p>The standard search does not follow any special cases.
   *
   * @param query The String query to search by
   * @param guild The Guild to search from
   * @return A possibly-empty {@link java.util.List List} of VoiceChannels found by the query from
   *     the provided Guild.
   */
  public static List<VoiceChannel> findVoiceChannels(String query, Guild guild) {
    if (DISCORD_ID.matcher(query).matches()) {
      VoiceChannel vc = guild.getVoiceChannelById(query);
      if (vc != null) {
        return Collections.singletonList(vc);
      }
    }
    return genericVoiceChannelSearch(query, guild.getVoiceChannelCache());
  }

  private static List<VoiceChannel> genericVoiceChannelSearch(
      String query, SnowflakeCacheView<VoiceChannel> cache) {
    List<VoiceChannel> exact = new ArrayList<>();
    List<VoiceChannel> wrongcase = new ArrayList<>();
    List<VoiceChannel> startswith = new ArrayList<>();
    List<VoiceChannel> contains = new ArrayList<>();
    String lowerquery = query.toLowerCase();
    cache.forEach(
        vc -> {
          String name = vc.getName();
          if (name.equals(query)) {
            exact.add(vc);
          } else if (name.equalsIgnoreCase(query) && exact.isEmpty()) {
            wrongcase.add(vc);
          } else if (name.toLowerCase().startsWith(lowerquery) && wrongcase.isEmpty()) {
            startswith.add(vc);
          } else if (name.toLowerCase().contains(lowerquery) && startswith.isEmpty()) {
            contains.add(vc);
          }
        });
    if (!exact.isEmpty()) {
      return Collections.unmodifiableList(exact);
    }
    if (!wrongcase.isEmpty()) {
      return Collections.unmodifiableList(wrongcase);
    }
    if (!startswith.isEmpty()) {
      return Collections.unmodifiableList(startswith);
    }
    return Collections.unmodifiableList(contains);
  }

  /**
   * Queries a provided {@link net.dv8tion.jda.api.entities.Guild Guild} for {@link
   * net.dv8tion.jda.api.entities.Role Role}s.
   *
   * <p>The following special case is applied before the standard search is done:
   *
   * <ul>
   *   <li>Role Mention: Query provided matches a @role mention (more specifically {@literal
   *       <@&roleID>})
   * </ul>
   *
   * @param query The String query to search by
   * @param guild The Guild to search from
   * @return A possibly-empty {@link java.util.List List} of Roles found by the query from the
   *     provided Guild.
   */
  public static List<Role> findRoles(String query, Guild guild) {
    final Matcher roleMention = ROLE_MENTION.matcher(query);
    if (roleMention.matches()) {
      final Role role = guild.getRoleById(roleMention.group(1));
      if (role != null) {
        return Collections.singletonList(role);
      }
    } else if (DISCORD_ID.matcher(query).matches()) {
      final Role role = guild.getRoleById(query);
      if (role != null) {
        return Collections.singletonList(role);
      }
    }
    final List<Role> exact = new ArrayList<>();
    final List<Role> wrongcase = new ArrayList<>();
    final List<Role> startswith = new ArrayList<>();
    final List<Role> contains = new ArrayList<>();
    final String lowerquery = query.toLowerCase();
    guild
        .getRoleCache()
        .forEach(
            role -> {
              String name = role.getName();
              if (name.equals(query)) {
                exact.add(role);
              } else if (name.equalsIgnoreCase(query) && exact.isEmpty()) {
                wrongcase.add(role);
              } else if (name.toLowerCase().startsWith(lowerquery) && wrongcase.isEmpty()) {
                startswith.add(role);
              } else if (name.toLowerCase().contains(lowerquery) && startswith.isEmpty()) {
                contains.add(role);
              }
            });
    if (!exact.isEmpty()) {
      return Collections.unmodifiableList(exact);
    }
    if (!wrongcase.isEmpty()) {
      return Collections.unmodifiableList(wrongcase);
    }
    if (!startswith.isEmpty()) {
      return Collections.unmodifiableList(startswith);
    }
    return Collections.unmodifiableList(contains);
  }

  // Prevent instantiation
  private FinderUtil() {}
}
