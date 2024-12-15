/*
 * Copyright 2016 John Grosh (jagrosh).
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
package com.jagrosh.jmusicbot;

import net.dv8tion.jda.api.Permission;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author John Grosh (jagrosh)
 */
@SpringBootApplication
public class JMusicBot {
  public static final Permission[] RECOMMENDED_PERMS = {
    Permission.MESSAGE_READ,
    Permission.MESSAGE_WRITE,
    Permission.MESSAGE_HISTORY,
    Permission.MESSAGE_ADD_REACTION,
    Permission.MESSAGE_EMBED_LINKS,
    Permission.MESSAGE_ATTACH_FILES,
    Permission.MESSAGE_MANAGE,
    Permission.MESSAGE_EXT_EMOJI,
    Permission.VOICE_CONNECT,
    Permission.VOICE_SPEAK,
    Permission.NICKNAME_CHANGE
  };

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    // TODO
    // ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
    //     .setLevel(Level.toLevel(config.getLogLevel(), Level.INFO));
    SpringApplication.run(JMusicBot.class, args);
  }
}
