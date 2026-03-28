package fr.dpmr.npc;

import fr.dpmr.game.LootManager;
import fr.dpmr.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns PNJ discrets dans la zone war et événement « hélicoptère » (escouade + coffre).
 */
public final class WarWorldNpcManager {

    private static final String SRC_AMBIENT = "ambient";
    private static final String SRC_HELICOPTER = "helicopter";

    private final JavaPlugin plugin;
    private final NpcSpawnerManager npcSpawnerManager;
    private final ZoneManager zoneManager;
    private final LootManager lootManager;
    private BukkitTask ambientTask;
    private BukkitTask helicopterTask;

    public WarWorldNpcManager(JavaPlugin plugin, NpcSpawnerManager npcSpawnerManager,
            ZoneManager zoneManager, LootManager lootManager) {
        this.plugin = plugin;
        this.npcSpawnerManager = npcSpawnerManager;
        this.zoneManager = zoneManager;
        this.lootManager = lootManager;
    }

    public void start() {
        stop();
        FileConfiguration cfg = plugin.getConfig();
        long ambientPeriod = Math.max(100L, cfg.getLong("npc-war-world.ambient.interval-ticks", 7200L));
        if (cfg.getBoolean("npc-war-world.ambient.enabled", true)) {
            ambientTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAmbient, ambientPeriod, ambientPeriod);
        }
        long heliSec = Math.max(60L, cfg.getLong("npc-war-world.helicopter.interval-seconds", 1800L));
        if (cfg.getBoolean("npc-war-world.helicopter.enabled", true)) {
            long periodTicks = heliSec * 20L;
            helicopterTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickHelicopter, periodTicks, periodTicks);
        }
    }

    public void stop() {
        if (ambientTask != null) {
            ambientTask.cancel();
            ambientTask = null;
        }
        if (helicopterTask != null) {
            helicopterTask.cancel();
            helicopterTask = null;
        }
    }

    public void reloadSchedules() {
        start();
    }

    /**
     * Déclenche l’événement hélicoptère (admin / test).
     *
     * @return {@code false} si aucun point de largage valide
     */
    public boolean forceHelicopterEvent() {
        return runHelicopterEvent();
    }

    private void tickAmbient() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("npc-war-world.ambient.enabled", true)) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > cfg.getDouble("npc-war-world.ambient.attempt-chance", 0.12)) {
            return;
        }
        boolean requireWar = cfg.getBoolean("npc-war-world.ambient.require-war-zone", true);
        int maxActive = Math.max(1, cfg.getInt("npc-war-world.ambient.max-active", 3));
        Location feet;
        if (requireWar) {
            if (!cfg.getBoolean("zones.war.enabled", false)) {
                return;
            }
            feet = zoneManager.pickRandomSurfaceFeetInWarZone(Math.max(20, cfg.getInt("npc-war-world.ambient.surface-attempts", 48)));
        } else {
            String wn = cfg.getString("npc-war-world.ambient.fallback-world", "world");
            if (Bukkit.getWorld(wn) == null) {
                return;
            }
            int spread = Math.max(16, cfg.getInt("npc-war-world.ambient.fallback-spread", 72));
            feet = zoneManager.pickRandomSurfaceNearWorldSpawn(wn, spread,
                    Math.max(24, cfg.getInt("npc-war-world.ambient.surface-attempts", 48)));
        }
        if (feet == null) {
            return;
        }
        if (npcSpawnerManager.countFakeNpcsWithSpawnSource(feet.getWorld(), SRC_AMBIENT) >= maxActive) {
            return;
        }
        String kind = cfg.getString("npc-war-world.ambient.kind", "MILITARY");
        int reward = Math.max(1, cfg.getInt("npc-war-world.ambient.reward-points", 4));
        int gMin = Math.max(0, cfg.getInt("npc-war-world.ambient.gold-min", 1));
        int gMax = Math.max(gMin, cfg.getInt("npc-war-world.ambient.gold-max", 3));
        int despawn = Math.max(400, cfg.getInt("npc-war-world.ambient.despawn-ticks", 5200));
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Location jitter = feet.clone().add(rnd.nextDouble(-2.8, 2.8), 0, rnd.nextDouble(-2.8, 2.8));
        npcSpawnerManager.spawnSingleCustomNpc(jitter, kind, reward, gMin, gMax, despawn, SRC_AMBIENT);
    }

    private void tickHelicopter() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("npc-war-world.helicopter.enabled", true)) {
            return;
        }
        runHelicopterEvent();
    }

    private boolean runHelicopterEvent() {
        FileConfiguration cfg = plugin.getConfig();
        boolean requireWar = cfg.getBoolean("npc-war-world.helicopter.require-war-zone", true);
        Location center;
        if (requireWar && cfg.getBoolean("zones.war.enabled", false)) {
            center = zoneManager.pickRandomSurfaceFeetInWarZone(Math.max(30, cfg.getInt("npc-war-world.helicopter.surface-attempts", 64)));
        } else if (requireWar) {
            return false;
        } else {
            String wn = cfg.getString("npc-war-world.helicopter.fallback-world", "world");
            int spread = Math.max(24, cfg.getInt("npc-war-world.helicopter.fallback-spread", 96));
            center = zoneManager.pickRandomSurfaceNearWorldSpawn(wn, spread,
                    Math.max(32, cfg.getInt("npc-war-world.helicopter.surface-attempts", 64)));
        }
        if (center == null || center.getWorld() == null) {
            return false;
        }
        World world = center.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int soldiersMin = Math.max(1, cfg.getInt("npc-war-world.helicopter.soldiers-min", 2));
        int soldiersMax = Math.max(soldiersMin, cfg.getInt("npc-war-world.helicopter.soldiers-max", 4));
        int count = rnd.nextInt(soldiersMin, soldiersMax + 1);
        double radius = Math.max(2.5, cfg.getDouble("npc-war-world.helicopter.spawn-radius", 5.5));
        int reward = Math.max(1, cfg.getInt("npc-war-world.helicopter.reward-points", 6));
        int gMin = Math.max(0, cfg.getInt("npc-war-world.helicopter.gold-min", 2));
        int gMax = Math.max(gMin, cfg.getInt("npc-war-world.helicopter.gold-max", 5));
        int despawn = Math.max(600, cfg.getInt("npc-war-world.helicopter.despawn-ticks", 7200));

        if (cfg.getBoolean("npc-war-world.helicopter.broadcast", true)) {
            Bukkit.broadcast(Component.text("[DPMR] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("Hélicoptère de guerre", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text(" — largage hostile près de ", NamedTextColor.GRAY))
                    .append(Component.text(world.getName() + " " + center.getBlockX() + " " + center.getBlockZ(),
                            NamedTextColor.YELLOW)));
        }
        world.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.55f);
        world.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.9f, 1.2f);

        for (int i = 0; i < count; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = rnd.nextDouble(0.4, radius);
            Location at = center.clone().add(Math.cos(ang) * dist, 0, Math.sin(ang) * dist);
            npcSpawnerManager.spawnSingleCustomNpc(at, "MILITARY", reward, gMin, gMax, despawn, SRC_HELICOPTER);
        }

        placeHelicopterChest(center.clone(), cfg);
        return true;
    }

    private void placeHelicopterChest(Location feet, FileConfiguration cfg) {
        World world = feet.getWorld();
        if (world == null) {
            return;
        }
        int x = feet.getBlockX();
        int z = feet.getBlockZ();
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int ox = cfg.getInt("npc-war-world.helicopter.chest-offset-x", 1);
        int oz = cfg.getInt("npc-war-world.helicopter.chest-offset-z", 1);
        int tx = x + ox;
        int tz = z + oz;
        int ty = world.getHighestBlockYAt(tx, tz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        Block chestBlock = world.getBlockAt(tx, ty + 1, tz);
        if (!chestBlock.getType().isAir()) {
            chestBlock = world.getBlockAt(x, y + 1, z);
        }
        if (!chestBlock.getType().isAir()) {
            return;
        }
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getState() instanceof Chest chest) {
            lootManager.fillHelicopterSupplyChest(chest.getBlockInventory());
            chest.update();
        }
        world.playSound(chestBlock.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.15f);
    }
}
