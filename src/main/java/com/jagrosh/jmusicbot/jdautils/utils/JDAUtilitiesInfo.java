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
 * Information regarding the library. <br>
 * Visit the JDA-Utilities <a href="https://github.com/JDA-Applications/JDA-Utilities">GitHub
 * Repository</a> to submit issue reports or feature requests, or join the <a
 * href="https://discord.gg/0hMr4ce0tIk3pSjp">Official JDA Discord Guild</a> if you need any
 * assistance with the library!
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public final class JDAUtilitiesInfo {
  public static final String VERSION_MAJOR = "@VERSION_MAJOR@";
  public static final String VERSION_MINOR = "@VERSION_MINOR@";
  public static final String VERSION_REVISION = "@VERSION_REVISION@";
  public static final String VERSION =
      VERSION_MAJOR.startsWith("@")
          ? "DEV"
          : VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_REVISION;

  private JDAUtilitiesInfo() {
    // hidden default constructor
  }
}
