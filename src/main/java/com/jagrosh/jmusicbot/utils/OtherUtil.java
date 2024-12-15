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
package com.jagrosh.jmusicbot.utils;

import com.jagrosh.jmusicbot.JMusicBot;
import com.jagrosh.jmusicbot.spring.exceptions.UnsupportedBotException;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.User;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class OtherUtil {
  private static final String WINDOWS_INVALID_PATH = "c:\\windows\\system32\\";

  /**
   * gets a Path from a String also fixes the windows tendency to try to start in system32 any time
   * the bot tries to access this path, it will instead start in the location of the jar file
   *
   * @param path the string path
   * @return the Path object
   */
  public static Path getPath(String path) {
    Path result = Paths.get(path);
    // special logic to prevent trying to access system32
    if (result.toAbsolutePath().toString().toLowerCase().startsWith(WINDOWS_INVALID_PATH)) {
      try {
        result =
            Paths.get(
                new File(
                            JMusicBot.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI())
                        .getParentFile()
                        .getPath()
                    + File.separator
                    + path);
      } catch (URISyntaxException ignored) {
        // do nothing
      }
    }
    return result;
  }

  /**
   * Parses an activity from a string
   *
   * @param game the game, including the action such as 'playing' or 'watching'
   * @return the parsed activity
   */
  public static Activity parseGame(String game) {
    if (game == null || game.trim().isEmpty() || game.trim().equalsIgnoreCase("default"))
      return null;
    String lower = game.toLowerCase();
    if (lower.startsWith("playing"))
      return Activity.playing(makeNonEmpty(game.substring(7).trim()));
    if (lower.startsWith("listening to"))
      return Activity.listening(makeNonEmpty(game.substring(12).trim()));
    if (lower.startsWith("listening"))
      return Activity.listening(makeNonEmpty(game.substring(9).trim()));
    if (lower.startsWith("watching"))
      return Activity.watching(makeNonEmpty(game.substring(8).trim()));
    if (lower.startsWith("streaming")) {
      String[] parts = game.substring(9).trim().split("\\s+", 2);
      if (parts.length == 2) {
        return Activity.streaming(makeNonEmpty(parts[1]), "https://twitch.tv/" + parts[0]);
      }
    }
    return Activity.playing(game);
  }

  public static String makeNonEmpty(String str) {
    return str == null || str.isEmpty() ? "\u200B" : str;
  }

  public static OnlineStatus parseStatus(String status) {
    if (status == null || status.trim().isEmpty()) return OnlineStatus.ONLINE;
    OnlineStatus st = OnlineStatus.fromKey(status);
    return st == null ? OnlineStatus.ONLINE : st;
  }

  /** Checks if the bot JMusicBot is being run on is supported & throws the reason if it is not. */
  public static void checkIfBotSupported(JDA jda) throws UnsupportedBotException {
    if (jda.getSelfUser().getFlags().contains(User.UserFlag.VERIFIED_BOT))
      throw new UnsupportedBotException(
          "The bot is verified. Using JMusicBot in a verified bot is not supported.");

    ApplicationInfo info = jda.retrieveApplicationInfo().complete();
    if (info.isBotPublic())
      throw new UnsupportedBotException(
          "\"Public Bot\" is enabled. Using JMusicBot as a public bot is not supported. Please disable it in the "
              + "Developer Dashboard at https://discord.com/developers/applications/"
              + jda.getSelfUser().getId()
              + "/bot ."
              + "You may also need to disable all Installation Contexts at https://discord.com/developers/applications/"
              + jda.getSelfUser().getId()
              + "/installation .");
  }

  private OtherUtil() {
    // hidden default constructor
  }
}
