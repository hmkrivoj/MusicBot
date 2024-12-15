package com.jagrosh.jmusicbot.spring;

import com.jagrosh.jmusicbot.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfiguration {
  // This sets the token for the bot to log in with
  // This MUST be a bot token (user tokens will not work)
  // If you don't know how to get a bot token, please see the guide here:
  // https://github.com/jagrosh/MusicBot/wiki/Getting-a-Bot-Token
  private String token;
  // This sets the owner of the bot
  // This needs to be the owner's ID (a 17-18 digit number)
  // https://github.com/jagrosh/MusicBot/wiki/Finding-Your-User-ID
  private Long owner;
  // This sets the prefix for the bot
  // The prefix is used to control the commands
  // If you use !!, the play command will be !!play
  // If you do not set this, the prefix will be a mention of the bot (@Botname play)
  private String prefix;
  // If you set this, it modifies the default game of the bot
  // Set this to NONE to have no game
  // Set this to DEFAULT to use the default game
  // You can make the game "Playing X", "Listening to X", or "Watching X"
  // where X is the title. If you don't include an action, it will use the
  // default of "Playing"
  private String game;
  // If you set this, it will modify the default status of bot
  // Valid values: ONLINE IDLE DND INVISIBLE
  private String status;
  // If you set this to true, the bot will list the title of the song it is currently playing in its
  // "Playing" status. Note that this will ONLY work if the bot is playing music on ONE guild;
  // if the bot is playing on multiple guilds, this will not work.
  private boolean songinstatus;
  // If you set this, the bot will also use this prefix in addition to
  // the one provided above
  private String altprefix;
  // If you set these, it will change the various emojis
  private String success;
  private String warning;
  private String error;
  private String loading;
  private String searching;
  // If you set this, you change the word used to view the help.
  // For example, if you set the prefix to !! and the help to cmds, you would type
  // !!cmds to see the help text
  private String help;
  // If you set this, the "nowplaying" command will show youtube thumbnails
  // Note: If you set this to true, the nowplaying boxes will NOT refresh
  // This is because refreshing the boxes causes the image to be reloaded
  // every time it refreshes.
  private boolean npimages;
  // If you set this, the bot will not leave a voice channel after it finishes a queue.
  // Keep in mind that being connected to a voice channel uses additional bandwith,
  // so this option is not recommended if bandwidth is a concern.
  private boolean stayinchannel;
  // This sets the maximum amount of seconds any track loaded can be. If not set or set
  // to any number less than or equal to zero, there is no maximum time length. This time
  // restriction applies to songs loaded from any source.
  private Long maxtime;
  // This sets the maximum number of pages of songs that can be loaded from a YouTube
  // playlist. Each page can contain up to 100 tracks. Playing a playlist with more
  // pages than the maximum will stop loading after the provided number of pages.
  // For example, if the max was set to 15 and a playlist contained 1850 tracks,
  // only the first 1500 tracks (15 pages) would be loaded. By default, this is
  // set to 10 pages (1000 tracks).
  private Integer maxytplaylistpages;
  // This sets the ratio of users that must vote to skip the currently playing song.
  // Guild owners can define their own skip ratios, but this will be used if a guild
  // has not defined their own skip ratio.
  private Double skipratio;
  // This sets the amount of seconds the bot will stay alone on a voice channel until it
  // automatically leaves the voice channel and clears the queue. If not set or set
  // to any number less than or equal to zero, the bot won't leave when alone.
  private Long alonetimeuntilstop;
  // This sets an alternative folder to be used as the Playlists folder
  // This can be a relative or absolute path
  private String playlistsfolder;
  // These settings allow you to configure custom aliases for all commands.
  // Multiple aliases may be given, separated by commas.
  //
  // Example 1: Giving command "play" the alias "p":
  // play = [ p ]
  //
  // Example 2: Giving command "search" the aliases "yts" and "find":
  // search = [ yts, find ]
  private Map<String, List<String>> aliases = new HashMap<>();
  // This sets the logging verbosity.
  // Available levels: off, error, warn, info, debug, trace, all
  //
  // It is recommended to leave this at info. Debug log levels might help with troubleshooting,
  // but can contain sensitive data.
  private String loglevel;
  // Transforms are used to modify specific play inputs and convert them to different kinds of inputs
  // These are quite complicated to use, and have limited use-cases, but in theory allow for rough
  // whitelists or blacklists, roundabout loading from some sources, and customization of how things are
  // requested.
  //
  // These are NOT EASY to set up, so if you want to use these, you'll need to look through the code
  // for how they work and what fields are needed. Also, it's possible this feature might get entirely
  // removed in the future if I find a better way to do this.
  private Map<String, TransformativeAudioSourceManagerConfig> transforms = new HashMap<>();

  private String poToken;
  private String visitorData;

  public String parseAltPrefix() {
    return "NONE".equalsIgnoreCase(altprefix) ? null : altprefix;
  }

  public OnlineStatus parseStatus() {
    return OtherUtil.parseStatus(status);
  }

  public Activity parseGame() {
    return OtherUtil.parseGame(game);
  }

  public boolean parseIsGameNone() {
    final var activity = parseGame();
    return activity != null && activity.getName().equalsIgnoreCase("none");
  }

  public boolean calcIsTooLong(AudioTrack track) {
    if (maxtime <= 0) return false;
    return Math.round(track.getDuration() / 1000.0) > maxtime;
  }


  @Data
  public static class TransformativeAudioSourceManagerConfig {
    private String regex;
    private String replacement;
    private String selector;
    private String format;
  }
}
