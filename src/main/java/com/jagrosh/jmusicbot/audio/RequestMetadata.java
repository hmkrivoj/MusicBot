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

import com.jagrosh.jmusicbot.jdautils.CommandEvent;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.User;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RequestMetadata {
  public static final RequestMetadata EMPTY = new RequestMetadata(null, null);

  public final UserInfo user;
  public final RequestInfo requestInfo;

  public RequestMetadata(User user, RequestInfo requestInfo) {
    this.user =
        user == null
            ? null
            : new UserInfo(
                user.getIdLong(),
                user.getName(),
                user.getDiscriminator(),
                user.getEffectiveAvatarUrl());
    this.requestInfo = requestInfo;
  }

  public long getOwner() {
    return user == null ? 0L : user.id;
  }

  public static RequestMetadata fromResultHandler(AudioTrack track, CommandEvent event) {
    return new RequestMetadata(
        event.getAuthor(), new RequestInfo(event.getArgs(), track.getInfo().uri));
  }

  public static class RequestInfo {
    public final long startTimestamp;

    public RequestInfo(String query, String url) {
      this(url, tryGetTimestamp(query));
    }

    private RequestInfo(String url, long startTimestamp) {
      this.startTimestamp = startTimestamp;
    }

    private static final Pattern youtubeTimestampPattern =
        Pattern.compile("youtu(?:\\.be|be\\..+)/.*\\?.*(?!.*list=)t=([\\dhms]+)");

    private static long tryGetTimestamp(String url) {
      Matcher matcher = youtubeTimestampPattern.matcher(url);
      return matcher.find() ? TimeUtil.parseUnitTime(matcher.group(1)) : 0;
    }
  }

  public static class UserInfo {
    public final long id;
    public final String username;
    public final String discrim;
    public final String avatar;

    private UserInfo(long id, String username, String discrim, String avatar) {
      this.id = id;
      this.username = username;
      this.discrim = discrim;
      this.avatar = avatar;
    }
  }
}
