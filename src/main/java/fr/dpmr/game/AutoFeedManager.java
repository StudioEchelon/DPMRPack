package fr.dpmr.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Faim auto-remplie, mais regen naturelle (satiated) désactivée.
 */
public class AutoFeedManager implements Listener {

    private final JavaPlugin plugin;
    private BukkitTask task;

    public AutoFeedManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getFoodLevel() < 20) {
                    p.setFoodLevel(20);
                }
                if (p.getSaturation() < 20f) {
                    p.setSaturation(20f);
                }
            }
        }, 20L, 40L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onNaturalRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }
}

