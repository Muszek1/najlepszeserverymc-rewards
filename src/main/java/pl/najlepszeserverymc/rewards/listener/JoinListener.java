package pl.najlepszeserverymc.rewards.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.najlepszeserverymc.rewards.service.RewardService;

public final class JoinListener implements Listener {

  private final RewardService rewardService;

  public JoinListener(RewardService rewardService) {
    this.rewardService = rewardService;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    rewardService.rewardPlayerIfPending(event.getPlayer());
  }
}
