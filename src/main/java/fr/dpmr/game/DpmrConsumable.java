package fr.dpmr.game;

import org.bukkit.Material;

public enum DpmrConsumable {
    BANDAGE_SMALL("bandage-small", Material.PAPER, true),
    BANDAGE_MEDIUM("bandage-medium", Material.PAPER, true),
    BANDAGE_LARGE("bandage-large", Material.PAPER, true),
    MEDIKIT("medikit", Material.PAPER, true),
    SHIELD_POTION_SMALL("shield-potion-small", Material.POTION, false),
    SHIELD_POTION_MEDIUM("shield-potion-medium", Material.POTION, false),
    SHIELD_POTION_LARGE("shield-potion-large", Material.POTION, false);

    private final String configKey;
    private final Material material;
    private final boolean heal;

    DpmrConsumable(String configKey, Material material, boolean heal) {
        this.configKey = configKey;
        this.material = material;
        this.heal = heal;
    }

    public String configKey() {
        return configKey;
    }

    public Material material() {
        return material;
    }

    public boolean heal() {
        return heal;
    }

    public static DpmrConsumable fromConfigKey(String key) {
        if (key == null) {
            return null;
        }
        String k = key.trim().replace('_', '-').toLowerCase();
        for (DpmrConsumable c : values()) {
            if (c.configKey.equals(k)) {
                return c;
            }
        }
        return null;
    }
}
