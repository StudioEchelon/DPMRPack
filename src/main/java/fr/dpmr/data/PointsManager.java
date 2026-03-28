package fr.dpmr.data;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class PointsManager {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private Function<UUID, Double> gainMultiplierProvider = uuid -> 1.0;

    public PointsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "points.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        points.clear();
        kills.clear();
        if (yaml.isConfigurationSection("players")) {
            for (String key : Objects.requireNonNull(yaml.getConfigurationSection("players")).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int value = yaml.getInt("players." + key, 0);
                    points.put(uuid, value);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("UUID invalide dans points.yml: " + key);
                }
            }
        }
        if (yaml.isConfigurationSection("kills")) {
            for (String key : Objects.requireNonNull(yaml.getConfigurationSection("kills")).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int value = yaml.getInt("kills." + key, 0);
                    kills.put(uuid, value);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("UUID invalide dans kills (points.yml): " + key);
                }
            }
        }
    }

    public void save() {
        yaml.set("players", null);
        for (Map.Entry<UUID, Integer> entry : points.entrySet()) {
            yaml.set("players." + entry.getKey(), entry.getValue());
        }
        yaml.set("kills", null);
        for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
            yaml.set("kills." + entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder points.yml: " + e.getMessage());
        }
    }

    public int getPoints(UUID uuid) {
        return points.getOrDefault(uuid, 0);
    }

    public void addPoints(UUID uuid, int amount) {
        int adjusted = amount;
        if (amount > 0) {
            double mult = Math.max(1.0, gainMultiplierProvider.apply(uuid));
            adjusted = (int) Math.round(amount * mult);
        }
        points.put(uuid, Math.max(0, getPoints(uuid) + adjusted));
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public void addKill(UUID uuid) {
        kills.put(uuid, getKills(uuid) + 1);
    }

    public List<Map.Entry<UUID, Integer>> getTop(int limit) {
        return points.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .toList();
    }

    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) {
        return kills.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .toList();
    }

    public String resolveName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
    }

    public void setGainMultiplierProvider(Function<UUID, Double> gainMultiplierProvider) {
        this.gainMultiplierProvider = gainMultiplierProvider != null ? gainMultiplierProvider : (uuid -> 1.0);
    }
}
