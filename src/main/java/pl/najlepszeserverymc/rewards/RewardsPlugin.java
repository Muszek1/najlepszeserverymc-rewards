package pl.najlepszeserverymc.rewards;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.najlepszeserverymc.rewards.api.ApiClient;
import pl.najlepszeserverymc.rewards.command.RewardsCommand;
import pl.najlepszeserverymc.rewards.config.PluginConfig;
import pl.najlepszeserverymc.rewards.listener.JoinListener;
import pl.najlepszeserverymc.rewards.service.RewardService;

public final class RewardsPlugin extends JavaPlugin {

  private PluginConfig pluginConfig;
  private ApiClient apiClient;
  private RewardService rewardService;
  private int pollTaskId = -1;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    reloadLocalConfig();

    this.apiClient = new ApiClient(this);
    this.rewardService = new RewardService(this, apiClient);

    PluginCommand command = getCommand("nsmcrewards");
    if (command != null) {
      RewardsCommand executor = new RewardsCommand(this);
      command.setExecutor(executor);
      command.setTabCompleter(executor);
    }

    Bukkit.getPluginManager().registerEvents(new JoinListener(rewardService), this);
    startPolling();

    getLogger().info("NajlepszeServeryMC Rewards włączony. API: " + pluginConfig.apiUrl());
  }

  @Override
  public void onDisable() {
    stopPolling();
  }

  public void reloadLocalConfig() {
    reloadConfig();
    this.pluginConfig = PluginConfig.from(getConfig());
  }

  public void restartPolling() {
    stopPolling();
    startPolling();
  }

  private void startPolling() {
    long ticks = Math.max(5, pluginConfig.pollIntervalSeconds()) * 20L;
    pollTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
        this,
        () -> rewardService.pollAndReward(),
        40L,
        ticks
    ).getTaskId();
  }

  private void stopPolling() {
    if (pollTaskId >= 0) {
      Bukkit.getScheduler().cancelTask(pollTaskId);
      pollTaskId = -1;
    }
  }

  public PluginConfig pluginConfig() {
    return pluginConfig;
  }

  public RewardService rewardService() {
    return rewardService;
  }

  public ApiClient apiClient() {
    return apiClient;
  }
}
