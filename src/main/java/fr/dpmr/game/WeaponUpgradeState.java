package fr.dpmr.game;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Voie + palier (1–5) stockés sur l'item.
 */
public record WeaponUpgradeState(WeaponUpgradePath path, int tier) {

    public static final WeaponUpgradeState NONE = new WeaponUpgradeState(null, 0);

    public boolean isEmpty() {
        return path == null || tier <= 0;
    }

    public static WeaponUpgradeState read(ItemStack stack, JavaPlugin plugin) {
        if (stack == null || !stack.hasItemMeta()) {
            return NONE;
        }
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        var keyPath = new org.bukkit.NamespacedKey(plugin, "dpmr_upgrade_path");
        var keyTier = new org.bukkit.NamespacedKey(plugin, "dpmr_upgrade_tier");
        String pathStr = pdc.get(keyPath, PersistentDataType.STRING);
        Integer tierVal = pdc.get(keyTier, PersistentDataType.INTEGER);
        WeaponUpgradePath p = WeaponUpgradePath.fromId(pathStr);
        int t = tierVal == null ? 0 : Math.min(5, Math.max(0, tierVal));
        if (p == null || t <= 0) {
            return NONE;
        }
        return new WeaponUpgradeState(p, t);
    }

    public static void write(ItemStack stack, WeaponUpgradeState state, JavaPlugin plugin) {
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        var keyPath = new org.bukkit.NamespacedKey(plugin, "dpmr_upgrade_path");
        var keyTier = new org.bukkit.NamespacedKey(plugin, "dpmr_upgrade_tier");
        if (state.isEmpty()) {
            pdc.remove(keyPath);
            pdc.remove(keyTier);
        } else {
            pdc.set(keyPath, PersistentDataType.STRING, state.path.name());
            pdc.set(keyTier, PersistentDataType.INTEGER, state.tier);
        }
        stack.setItemMeta(meta);
    }
}
