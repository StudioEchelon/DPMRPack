package fr.dpmr.armor;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum ArmorProfile {
    ASSAULT("Assaut", NamedTextColor.GREEN, 0.88, 0.95, 0.92, 0.9),
    HEAVY("Lourd", NamedTextColor.GRAY, 0.78, 0.8, 0.9, 0.72),
    BREACHER("Anti-pompe", NamedTextColor.GOLD, 0.9, 0.55, 0.95, 0.88),
    MARKSMAN("Anti-sniper", NamedTextColor.AQUA, 0.9, 0.96, 0.62, 0.86),
    EOD("EOD", NamedTextColor.DARK_RED, 0.9, 0.94, 0.94, 0.55);

    private final String label;
    private final NamedTextColor color;
    private final double generalMul;
    private final double shotgunMul;
    private final double sniperMul;
    private final double explosiveMul;

    ArmorProfile(String label, NamedTextColor color, double generalMul, double shotgunMul, double sniperMul, double explosiveMul) {
        this.label = label;
        this.color = color;
        this.generalMul = generalMul;
        this.shotgunMul = shotgunMul;
        this.sniperMul = sniperMul;
        this.explosiveMul = explosiveMul;
    }

    public String label() {
        return label;
    }

    public NamedTextColor color() {
        return color;
    }

    public double multiplier(fr.dpmr.game.WeaponDamageType type) {
        return switch (type) {
            case SHOTGUN -> shotgunMul;
            case SNIPER -> sniperMul;
            case EXPLOSIVE -> explosiveMul;
            default -> generalMul;
        };
    }

    public Material helmetMaterial() {
        return switch (this) {
            case ASSAULT, BREACHER -> Material.IRON_HELMET;
            case HEAVY, EOD -> Material.NETHERITE_HELMET;
            case MARKSMAN -> Material.CHAINMAIL_HELMET;
        };
    }

    public Material chestMaterial() {
        return switch (this) {
            case ASSAULT, BREACHER -> Material.IRON_CHESTPLATE;
            case HEAVY, EOD -> Material.NETHERITE_CHESTPLATE;
            case MARKSMAN -> Material.CHAINMAIL_CHESTPLATE;
        };
    }

    public Material legsMaterial() {
        return switch (this) {
            case ASSAULT, BREACHER -> Material.IRON_LEGGINGS;
            case HEAVY, EOD -> Material.NETHERITE_LEGGINGS;
            case MARKSMAN -> Material.CHAINMAIL_LEGGINGS;
        };
    }

    public Material bootsMaterial() {
        return switch (this) {
            case ASSAULT, BREACHER -> Material.IRON_BOOTS;
            case HEAVY, EOD -> Material.NETHERITE_BOOTS;
            case MARKSMAN -> Material.CHAINMAIL_BOOTS;
        };
    }

    public static ArmorProfile fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return valueOf(id.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

