package fr.dpmr.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Positions des lodestones enregistrées comme tables d'armement.
 */
public final class ModificationTableRegistry {

    private static final String CONFIG_KEY = "mod-tables.locations";

    private final JavaPlugin plugin;
    private final Set<String> keys = new HashSet<>();
    private final NamespacedKey crystalKey;

    public ModificationTableRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.crystalKey = new NamespacedKey(plugin, "dpmr_modtable_crystal");
    }

    public void load() {
        keys.clear();
        FileConfiguration cfg = plugin.getConfig();
        List<String> list = cfg.getStringList(CONFIG_KEY);
        for (String line : list) {
            if (line != null && !line.isBlank()) {
                keys.add(line.trim());
            }
        }
        ensureAllCrystals();
    }

    public void save() {
        plugin.getConfig().set(CONFIG_KEY, keys.stream().sorted().toList());
        plugin.saveConfig();
    }

    public boolean add(Block block) {
        String k = encode(block.getLocation());
        if (keys.add(k)) {
            save();
            ensureCrystalAt(block);
            return true;
        }
        ensureCrystalAt(block);
        return false;
    }

    public boolean remove(Block block) {
        if (keys.remove(encode(block.getLocation()))) {
            save();
            removeCrystalAt(block);
            return true;
        }
        removeCrystalAt(block);
        return false;
    }

    public boolean isTable(Block block) {
        return keys.contains(encode(block.getLocation()));
    }

    public int count() {
        return keys.size();
    }

    public static String encode(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public static Location decode(JavaPlugin plugin, String key) {
        String[] p = key.split(":");
        if (p.length != 4) {
            return null;
        }
        World w = Bukkit.getWorld(p[0]);
        if (w == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            int z = Integer.parseInt(p[3]);
            return new Location(w, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<String> listKeys() {
        return keys.stream().sorted().toList();
    }

    private void ensureAllCrystals() {
        for (String key : keys) {
            Location loc = decode(plugin, key);
            if (loc != null && loc.getWorld() != null) {
                ensureCrystalAt(loc.getBlock());
            }
        }
    }

    private void ensureCrystalAt(Block tableBlock) {
        if (tableBlock == null || tableBlock.getWorld() == null) {
            return;
        }
        Location base = tableBlock.getLocation().add(0.5, 1.0, 0.5);
        for (Entity nearby : tableBlock.getWorld().getNearbyEntities(base, 0.4, 0.8, 0.4)) {
            if (nearby instanceof EnderCrystal crystal) {
                Byte marker = crystal.getPersistentDataContainer().get(crystalKey, PersistentDataType.BYTE);
                if (marker != null && marker == (byte) 1) {
                    return;
                }
            }
        }
        EnderCrystal crystal = tableBlock.getWorld().spawn(base, EnderCrystal.class);
        crystal.setShowingBottom(false);
        crystal.setInvulnerable(true);
        crystal.setBeamTarget(null);
        crystal.getPersistentDataContainer().set(crystalKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void removeCrystalAt(Block tableBlock) {
        if (tableBlock == null || tableBlock.getWorld() == null) {
            return;
        }
        Location base = tableBlock.getLocation().add(0.5, 1.0, 0.5);
        for (Entity nearby : tableBlock.getWorld().getNearbyEntities(base, 0.7, 1.2, 0.7)) {
            if (!(nearby instanceof EnderCrystal crystal)) {
                continue;
            }
            Byte marker = crystal.getPersistentDataContainer().get(crystalKey, PersistentDataType.BYTE);
            if (marker != null && marker == (byte) 1) {
                crystal.remove();
            }
        }
    }
}
