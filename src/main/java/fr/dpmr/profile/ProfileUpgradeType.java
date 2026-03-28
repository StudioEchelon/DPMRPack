package fr.dpmr.profile;

import org.bukkit.Material;

public enum ProfileUpgradeType {
    VITALITY("Vitalite", Material.APPLE, 10, 5),
    ARMOR("Armure", Material.IRON_CHESTPLATE, 12, 5),
    REGEN("Regeneration", Material.GLISTERING_MELON_SLICE, 15, 4),
    DAMAGE("Degats", Material.IRON_SWORD, 14, 5),
    ECONOMY("Economie", Material.GOLD_INGOT, 16, 4);

    private final String display;
    private final Material icon;
    private final int baseCost;
    private final int maxLevel;

    ProfileUpgradeType(String display, Material icon, int baseCost, int maxLevel) {
        this.display = display;
        this.icon = icon;
        this.baseCost = baseCost;
        this.maxLevel = maxLevel;
    }

    public String display() {
        return display;
    }

    public Material icon() {
        return icon;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public int nextCost(int currentLevel) {
        return baseCost + currentLevel * (baseCost / 2 + 4);
    }
}

