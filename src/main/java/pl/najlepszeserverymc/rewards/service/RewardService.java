package pl.najlepszeserverymc.rewards.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pl.najlepszeserverymc.rewards.RewardsPlugin;
import pl.najlepszeserverymc.rewards.api.ApiClient;
import pl.najlepszeserverymc.rewards.api.PendingVote;

public final class RewardService {

  private final RewardsPlugin plugin;
  private final ApiClient apiClient;
  private final ConcurrentHashMap<String, Boolean> inFlight = new ConcurrentHashMap<>();

  public RewardService(RewardsPlugin plugin, ApiClient apiClient) {
    this.plugin = plugin;
    this.apiClient = apiClient;
  }

  public void pollAndReward() {
    if (!plugin.pluginConfig().hasToken()) {
      return;
    }

    try {
      List<PendingVote> pending = apiClient.fetchPending();
      if (pending.isEmpty()) {
        return;
      }

      List<PendingVote> deliverable = new ArrayList<>();
      for (PendingVote vote : pending) {
        Player player = findOnline(vote.username());
        if (player != null) {
          deliverable.add(vote);
        }
      }

      if (deliverable.isEmpty()) {
        return;
      }

      Bukkit.getScheduler().runTask(plugin, () -> deliverSync(deliverable));
    } catch (Exception exception) {
      apiClient.logQuietFailure("Poll API", exception);
    }
  }

  public void rewardPlayerIfPending(Player player) {
    if (!plugin.pluginConfig().hasToken()) {
      return;
    }
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        List<PendingVote> pending = apiClient.fetchPending();
        List<PendingVote> mine = pending.stream()
            .filter(vote -> vote.username().equalsIgnoreCase(player.getName()))
            .toList();
        if (mine.isEmpty()) {
          return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
          if (player.isOnline()) {
            deliverSync(mine);
          }
        });
      } catch (Exception exception) {
        apiClient.logQuietFailure("Join poll", exception);
      }
    });
  }

  public void testReward(Player player) {
    runCommands(player);
    sendRewardedMessage(player);
  }

  private void deliverSync(List<PendingVote> votes) {
    List<String> claimedIds = new ArrayList<>();
    for (PendingVote vote : votes) {
      if (inFlight.putIfAbsent(vote.id(), Boolean.TRUE) != null) {
        continue;
      }
      try {
        Player player = findOnline(vote.username());
        if (player == null) {
          continue;
        }
        runCommands(player);
        sendRewardedMessage(player);
        claimedIds.add(vote.id());
        plugin.getLogger().info("Nagroda dla " + player.getName() + " (id=" + vote.id() + ")");
      } finally {
        inFlight.remove(vote.id());
      }
    }

    if (claimedIds.isEmpty()) {
      return;
    }

    List<String> toClaim = List.copyOf(claimedIds);
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        int claimed = apiClient.claim(toClaim);
        plugin.getLogger().info("Claim API: " + claimed + "/" + toClaim.size());
      } catch (Exception exception) {
        plugin.getLogger().log(Level.SEVERE, "Nie udało się oznaczyć nagród jako odebrane — mogą spaść ponownie!", exception);
      }
    });
  }

  private void runCommands(Player player) {
    for (String template : plugin.pluginConfig().commands()) {
      String command = template.replace("%player%", player.getName()).trim();
      if (command.isEmpty()) {
        continue;
      }
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
  }

  private void sendRewardedMessage(Player player) {
    String raw = plugin.pluginConfig().prefix() + plugin.pluginConfig().rewardedMessage();
    player.sendMessage(color(raw));
  }

  private static Player findOnline(String username) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.getName().equalsIgnoreCase(username)) {
        return player;
      }
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  private static String color(String input) {
    return ChatColor.translateAlternateColorCodes('&', input);
  }

  public String statusLine() {
    var config = plugin.pluginConfig();
    return "API=" + config.apiUrl()
        + " token=" + (config.hasToken() ? "OK" : "BRAK")
        + " poll=" + config.pollIntervalSeconds() + "s"
        + " cmds=" + config.commands().size()
        + " online=" + Bukkit.getOnlinePlayers().size();
  }
}
