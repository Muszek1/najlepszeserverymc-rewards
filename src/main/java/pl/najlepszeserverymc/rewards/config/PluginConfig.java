package pl.najlepszeserverymc.rewards.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginConfig {

  private final String apiUrl;
  private final String token;
  private final int pollIntervalSeconds;
  private final int batchSize;
  private final List<String> commands;
  private final String rewardedMessage;
  private final String prefix;

  private PluginConfig(
      String apiUrl,
      String token,
      int pollIntervalSeconds,
      int batchSize,
      List<String> commands,
      String rewardedMessage,
      String prefix
  ) {
    this.apiUrl = apiUrl;
    this.token = token;
    this.pollIntervalSeconds = pollIntervalSeconds;
    this.batchSize = batchSize;
    this.commands = commands;
    this.rewardedMessage = rewardedMessage;
    this.prefix = prefix;
  }

  public static PluginConfig from(FileConfiguration config) {
    String apiUrl = config.getString("api-url", "https://najlepszeserverymc.pl/api/v1/plugin");
    if (apiUrl.endsWith("/")) {
      apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
    }

    List<String> commands = new ArrayList<>(config.getStringList("commands"));
    if (commands.isEmpty()) {
      commands.add("say %player% zagłosował na najlepszeserverymc.pl!");
    }

    return new PluginConfig(
        apiUrl,
        config.getString("token", "").trim(),
        Math.max(5, config.getInt("poll-interval-seconds", 15)),
        Math.min(100, Math.max(1, config.getInt("batch-size", 50))),
        Collections.unmodifiableList(commands),
        config.getString("messages.rewarded", "&aDzięki za głos!"),
        config.getString("messages.prefix", "&8[&aNSMC&8]&r ")
    );
  }

  public String apiUrl() {
    return apiUrl;
  }

  public String token() {
    return token;
  }

  public boolean hasToken() {
    return !token.isEmpty() && !token.equalsIgnoreCase("WSTAW_TOKEN_Z_PANELU");
  }

  public int pollIntervalSeconds() {
    return pollIntervalSeconds;
  }

  public int batchSize() {
    return batchSize;
  }

  public List<String> commands() {
    return commands;
  }

  public String rewardedMessage() {
    return rewardedMessage;
  }

  public String prefix() {
    return prefix;
  }
}
