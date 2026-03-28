package fr.dpmr.cosmetics;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;

public enum CosmeticProfile {
    // Shot trails
    SHOT_HEART("shot_heart", CosmeticType.SHOT, "Tir: Coeurs", 120, Material.POPPY, Particle.HEART, NamedTextColor.RED),
    SHOT_END_ROD("shot_endrod", CosmeticType.SHOT, "Tir: End Rod", 160, Material.END_ROD, Particle.END_ROD, NamedTextColor.LIGHT_PURPLE),
    SHOT_SOUL("shot_soul", CosmeticType.SHOT, "Tir: Ames", 140, Material.SOUL_SOIL, Particle.SOUL, NamedTextColor.AQUA),
    SHOT_ELECTRIC("shot_electric", CosmeticType.SHOT, "Tir: Etincelles", 180, Material.LIGHTNING_ROD, Particle.ELECTRIC_SPARK, NamedTextColor.YELLOW),

    // Auras
    AURA_ENCHANT("aura_enchant", CosmeticType.AURA, "Aura: Enchant", 220, Material.ENCHANTED_BOOK, Particle.ENCHANT, NamedTextColor.LIGHT_PURPLE),
    AURA_CLOUD("aura_cloud", CosmeticType.AURA, "Aura: Nuage", 180, Material.WHITE_DYE, Particle.CLOUD, NamedTextColor.WHITE),
    AURA_FLAME("aura_flame", CosmeticType.AURA, "Aura: Flammes", 260, Material.BLAZE_POWDER, Particle.FLAME, NamedTextColor.GOLD),
    AURA_SNOW("aura_snow", CosmeticType.AURA, "Aura: Neige", 200, Material.SNOWBALL, Particle.SNOWFLAKE, NamedTextColor.AQUA),

    // Parachute FX
    PARA_CLOUD("para_cloud", CosmeticType.PARACHUTE, "Parachute: Nuage", 220, Material.WHITE_BANNER, Particle.CLOUD, NamedTextColor.WHITE),
    PARA_CHERRY("para_cherry", CosmeticType.PARACHUTE, "Parachute: Petales", 260, Material.PINK_PETALS, Particle.CHERRY_LEAVES, NamedTextColor.LIGHT_PURPLE),
    PARA_ENDROD("para_endrod", CosmeticType.PARACHUTE, "Parachute: End Rod", 300, Material.END_ROD, Particle.END_ROD, NamedTextColor.AQUA),

    // Vanity (items)
    VANITY_CROWN("crown", CosmeticType.VANITY, "Couronne", 300, Material.GOLDEN_HELMET, null, NamedTextColor.GOLD),
    VANITY_WINGS("wings", CosmeticType.VANITY, "Ailes", 420, Material.ELYTRA, null, NamedTextColor.AQUA),
    VANITY_CAPE("cape", CosmeticType.VANITY, "Cape", 360, Material.LEATHER_CHESTPLATE, null, NamedTextColor.DARK_PURPLE),
    VANITY_COLLAR("collar", CosmeticType.VANITY, "Collier", 240, Material.CHAIN, null, NamedTextColor.YELLOW);

    private final String id;
    private final CosmeticType type;
    private final String displayName;
    private final int price;
    private final Material icon;
    private final Particle particle;
    private final NamedTextColor color;

    CosmeticProfile(String id, CosmeticType type, String displayName, int price, Material icon, Particle particle, NamedTextColor color) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.price = Math.max(0, price);
        this.icon = icon;
        this.particle = particle;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public CosmeticType type() {
        return type;
    }

    public String displayName() {
        return displayName;
    }

    public int price() {
        return price;
    }

    public Material icon() {
        return icon;
    }

    public Particle particle() {
        return particle;
    }

    public NamedTextColor color() {
        return color;
    }

    public static CosmeticProfile fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.trim().toLowerCase();
        for (CosmeticProfile p : values()) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }
}

