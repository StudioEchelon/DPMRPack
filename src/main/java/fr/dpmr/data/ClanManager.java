package fr.dpmr.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClanManager {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<String, Set<UUID>> clans = new HashMap<>();
    private final Map<UUID, String> playerClan = new HashMap<>();

    public ClanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "clans.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        clans.clear();
        playerClan.clear();
        if (!yaml.isConfigurationSection("clans")) {
            return;
        }
        for (String clanName : Objects.requireNonNull(yaml.getConfigurationSection("clans")).getKeys(false)) {
            Set<UUID> members = new HashSet<>();
            List<String> rawMembers = yaml.getStringList("clans." + clanName + ".members");
            for (String raw : rawMembers) {
                try {
                    UUID uuid = UUID.fromString(raw);
                    members.add(uuid);
                    playerClan.put(uuid, clanName);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("UUID invalide dans clans.yml: " + raw);
                }
            }
            clans.put(clanName, members);
        }
    }

    public void save() {
        yaml.set("clans", null);
        for (Map.Entry<String, Set<UUID>> entry : clans.entrySet()) {
            List<String> members = entry.getValue().stream().map(UUID::toString).toList();
            yaml.set("clans." + entry.getKey() + ".members", members);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder clans.yml: " + e.getMessage());
        }
    }

    public boolean createClan(String clanName, UUID creator) {
        if (clans.containsKey(clanName) || playerClan.containsKey(creator)) {
            return false;
        }
        Set<UUID> members = new HashSet<>();
        members.add(creator);
        clans.put(clanName, members);
        playerClan.put(creator, clanName);
        return true;
    }

    public boolean joinClan(String clanName, UUID player) {
        Set<UUID> members = clans.get(clanName);
        if (members == null || playerClan.containsKey(player)) {
            return false;
        }
        members.add(player);
        playerClan.put(player, clanName);
        return true;
    }

    public boolean leaveClan(UUID player) {
        String clan = playerClan.remove(player);
        if (clan == null) {
            return false;
        }
        Set<UUID> members = clans.get(clan);
        if (members != null) {
            members.remove(player);
            if (members.isEmpty()) {
                clans.remove(clan);
            }
        }
        return true;
    }

    public String getPlayerClan(UUID player) {
        return playerClan.get(player);
    }

    public Map<String, Set<UUID>> getClans() {
        return Collections.unmodifiableMap(clans);
    }
}
