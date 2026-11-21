package pl.najlepszeserverymc.rewards.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.najlepszeserverymc.rewards.RewardsPlugin;

public final class RewardsCommand implements CommandExecutor, TabCompleter {

  private final RewardsPlugin plugin;

  public RewardsCommand(RewardsPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args
  ) {
    if (!sender.hasPermission("nsmcrewards.admin")) {
      sender.sendMessage(ChatColor.RED + "Brak uprawnień.");
      return true;
    }

    if (args.length == 0) {
      sender.sendMessage(ChatColor.GRAY + "/" + label + " <reload|status|test|poll> [nick]");
      return true;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "reload" -> {
        plugin.reloadLocalConfig();
        plugin.restartPolling();
        sender.sendMessage(ChatColor.GREEN + "Przeładowano config i restart pollera.");
      }
      case "status" -> sender.sendMessage(ChatColor.AQUA + plugin.rewardService().statusLine());
      case "poll" -> {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
          plugin.rewardService().pollAndReward();
          Bukkit.getScheduler().runTask(plugin, () ->
              sender.sendMessage(ChatColor.GREEN + "Ręczny poll zakończony (szczegóły w konsoli)."));
        });
      }
      case "test" -> {
        Player target;
        if (args.length >= 2) {
          target = Bukkit.getPlayerExact(args[1]);
          if (target == null) {
            sender.sendMessage(ChatColor.RED + "Gracz offline: " + args[1]);
            return true;
          }
        } else if (sender instanceof Player player) {
          target = player;
        } else {
          sender.sendMessage(ChatColor.RED + "Podaj nick: /" + label + " test <nick>");
          return true;
        }
        plugin.rewardService().testReward(target);
        sender.sendMessage(ChatColor.GREEN + "Wysłano testową nagrodę do " + target.getName());
      }
      default -> sender.sendMessage(ChatColor.GRAY + "/" + label + " <reload|status|test|poll> [nick]");
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args
  ) {
    List<String> out = new ArrayList<>();
    if (!sender.hasPermission("nsmcrewards.admin")) {
      return out;
    }
    if (args.length == 1) {
      for (String option : List.of("reload", "status", "test", "poll")) {
        if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
          out.add(option);
        }
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
      String prefix = args[1].toLowerCase(Locale.ROOT);
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
          out.add(player.getName());
        }
      }
    }
    return out;
  }
}
