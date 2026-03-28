package fr.dpmr.game;

import fr.dpmr.data.PointsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TopHologramManager {

    private final JavaPlugin plugin;
    private final PointsManager pointsManager;
    private final File file;
    private YamlConfiguration yaml;
    private final List<ArmorStand[]> columns = new ArrayList<>();
    private BukkitTask task;

    public TopHologramManager(JavaPlugin plugin, PointsManager pointsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
        loadYaml();
        respawnAll();
    }

    private void loadYaml() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 100L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        List<Map.Entry<UUID, Integer>> top = pointsManager.getTop(10);
        for (ArmorStand[] stands : columns) {
            if (stands == null || stands.length < 11) {
                continue;
            }
            stands[0].customName(Component.text("TOP 10 DPMR", NamedTextColor.GOLD));
            for (int i = 0; i < 10; i++) {
                if (i < top.size()) {
                    Map.Entry<UUID, Integer> e = top.get(i);
                    String name = pointsManager.resolveName(e.getKey());
                    stands[i + 1].customName(Component.text((i + 1) + ". " + name + " — " + e.getValue(), NamedTextColor.YELLOW));
                } else {
                    stands[i + 1].customName(Component.text("-", NamedTextColor.DARK_GRAY));
                }
            }
        }
    }

    public void addColumn(Player player) {
        Location feet = player.getLocation().getBlock().getLocation();
        Location base = feet.clone().add(0.5, 2.4, 0.5);
        List<String> list = new ArrayList<>(yaml.getStringList("locations"));
        list.add(encode(feet));
        yaml.set("locations", list);
        saveYaml();
        ArmorStand[] stands = spawnStands(base);
        columns.add(stands);
        player.sendMessage(Component.text("Hologramme top 10 pose ici.", NamedTextColor.GREEN));
    }

    private void respawnAll() {
        columns.clear();
        for (String raw : yaml.getStringList("locations")) {
            Location loc = decode(raw);
            if (loc != null) {
                columns.add(spawnStands(loc));
            }
        }
    }

    private static ArmorStand[] spawnStands(Location base) {
        World w = base.getWorld();
        if (w == null) {
            return new ArmorStand[0];
        }
        ArmorStand[] stands = new ArmorStand[11];
        for (int i = 0; i < 11; i++) {
            Location line = base.clone().add(0, -i * 0.28, 0);
            ArmorStand as = w.spawn(line, ArmorStand.class);
            as.setGravity(false);
            as.setInvisible(true);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.customName(Component.text(i == 0 ? "TOP 10" : "-", NamedTextColor.GRAY));
            stands[i] = as;
        }
        return stands;
    }

    /** Bloc sous l'hologramme (pieds du joueur). */
    private static String encode(Location blockFeet) {
        return blockFeet.getWorld().getName() + ";" + blockFeet.getBlockX() + ";" + blockFeet.getBlockY() + ";" + blockFeet.getBlockZ();
    }

    private static Location decode(String raw) {
        String[] p = raw.split(";");
        if (p.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(p[0]);
        if (world == null) {
            return null;
        }
        try {
            int bx = Integer.parseInt(p[1]);
            int by = Integer.parseInt(p[2]);
            int bz = Integer.parseInt(p[3]);
            return new Location(world, bx + 0.5, by + 2.4, bz + 0.5);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void saveYaml() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("holograms.yml: " + e.getMessage());
        }
    }

    public int columnCount() {
        return columns.size();
    }

    /**
     * Retire l'hologramme top 10 le plus proche (dans le rayon indique).
     *
     * @return true si une colonne a ete supprimee
     */
    public boolean removeNearestColumn(Player player, double maxDistance) {
        double maxSq = maxDistance * maxDistance;
        int bestIdx = -1;
        double bestD = maxSq;
        for (int i = 0; i < columns.size(); i++) {
            ArmorStand[] stands = columns.get(i);
            if (stands == null || stands.length < 1 || stands[0] == null || !stands[0].isValid()) {
                continue;
            }
            double d = stands[0].getLocation().distanceSquared(player.getLocation());
            if (d < bestD) {
                bestD = d;
                bestIdx = i;
            }
        }
        if (bestIdx < 0) {
            return false;
        }
        ArmorStand[] remove = columns.remove(bestIdx);
        if (remove != null) {
            for (ArmorStand as : remove) {
                if (as != null && as.isValid()) {
                    as.remove();
                }
            }
        }
        List<String> locs = new ArrayList<>(yaml.getStringList("locations"));
        if (bestIdx >= 0 && bestIdx < locs.size()) {
            locs.remove(bestIdx);
        }
        yaml.set("locations", locs);
        saveYaml();
        player.sendMessage(Component.text("Hologramme top 10 retire.", NamedTextColor.GREEN));
        return true;
    }
}
