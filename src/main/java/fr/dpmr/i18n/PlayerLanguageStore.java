package fr.dpmr.i18n;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerLanguageStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, GameLocale> cache = new ConcurrentHashMap<>();

    public PlayerLanguageStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player_languages.yml");
        load();
    }

    public GameLocale get(UUID uuid) {
        return cache.getOrDefault(uuid, GameLocale.EN);
    }

    public GameLocale get(Player player) {
        return get(player.getUniqueId());
    }

    public void set(Player player, GameLocale locale) {
        cache.put(player.getUniqueId(), locale);
        save();
    }

    private void load() {
        cache.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                cache.put(id, GameLocale.fromString(yaml.getString(key, "EN")));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, GameLocale> e : cache.entrySet()) {
            yaml.set(e.getKey().toString(), e.getValue().name());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save player_languages.yml: " + ex.getMessage());
        }
    }
}
