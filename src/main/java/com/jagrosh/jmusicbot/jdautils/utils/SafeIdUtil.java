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

/**
 * A Utilities class for safely checking and converting String IDs to longs usable with {@link
 * net.dv8tion.jda.api.utils.MiscUtil#parseSnowflake(String) MiscUtil.parseSnowflake(String)}, a
 * utility used in several {@code Object#getXById(String)} methods.
 *
 * <p>This class contains two static methods:
 *
 * <ul>
 *   <li>{@link SafeIdUtil#safeConvert(String) SafeIdUtil.safeConvert(String)} - Safely converts a
 *       String to a format usable with {@code MiscUtil.parseSnowflake(String)}.
 *   <li>{@link SafeIdUtil#checkId(String) SafeIdUtil.checkId(String)} - Checks if a String is safe
 *       to use with {@code MiscUtil.parseSnowflake(String)} as it is.
 * </ul>
 *
 * @since 1.2
 * @author Kaidan Gustave
 */
public final class SafeIdUtil {

  /**
   * Checks if the provided String ID is usable with {@link
   * net.dv8tion.jda.api.utils.MiscUtil#parseSnowflake(String) MiscUtil.parseSnowflake(String)}.
   *
   * @param id The String ID to be converted
   * @return {@code true} if both of the following criteria are not met:
   *     <ul>
   *       <li>If the provided String throws a {@link java.lang.NumberFormatException
   *           NumberFormatException} when used with {@link java.lang.Long#parseLong(String)
   *           Long.parseLong(String)}.
   *       <li>If the provided String is converted, but the returned {@code long} is negative.
   *     </ul>
   */
  public static boolean checkId(String id) {
    try {
      long l = Long.parseLong(id.trim());
      return l >= 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  // Prevent instantiation
  private SafeIdUtil() {}
}
