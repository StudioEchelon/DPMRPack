package fr.dpmr.game;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public record JerrycanUpgradeState(JerrycanUpgradePath path, int tier) {

    public static final JerrycanUpgradeState NONE = new JerrycanUpgradeState(null, 0);

    public boolean isEmpty() {
        return path == null || tier <= 0;
    }

    public static JerrycanUpgradeState read(ItemStack stack, JavaPlugin plugin) {
        if (stack == null || !stack.hasItemMeta()) {
            return NONE;
        }
        return readPdc(stack.getItemMeta().getPersistentDataContainer(), plugin);
    }

    public static JerrycanUpgradeState readPdc(PersistentDataContainer pdc, JavaPlugin plugin) {
        var keyPath = new org.bukkit.NamespacedKey(plugin, "dpmr_jerry_path");
        var keyTier = new org.bukkit.NamespacedKey(plugin, "dpmr_jerry_tier");
        String pathStr = pdc.get(keyPath, PersistentDataType.STRING);
        Integer tierVal = pdc.get(keyTier, PersistentDataType.INTEGER);
        JerrycanUpgradePath p = JerrycanUpgradePath.fromId(pathStr);
        int t = tierVal == null ? 0 : Math.min(5, Math.max(0, tierVal));
        if (p == null || t <= 0) {
            return NONE;
        }
        return new JerrycanUpgradeState(p, t);
    }

    public static void write(ItemStack stack, JerrycanUpgradeState state, JavaPlugin plugin) {
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        writePdc(meta.getPersistentDataContainer(), state, plugin);
        stack.setItemMeta(meta);
    }

    public static void writePdc(PersistentDataContainer pdc, JerrycanUpgradeState state, JavaPlugin plugin) {
        var keyPath = new org.bukkit.NamespacedKey(plugin, "dpmr_jerry_path");
        var keyTier = new org.bukkit.NamespacedKey(plugin, "dpmr_jerry_tier");
        if (state.isEmpty()) {
            pdc.remove(keyPath);
            pdc.remove(keyTier);
        } else {
            pdc.set(keyPath, PersistentDataType.STRING, state.path.name());
            pdc.set(keyTier, PersistentDataType.INTEGER, state.tier);
        }
    }
}
