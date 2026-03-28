package fr.dpmr.game;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Types de familiers de combat / soutien.
 */
public enum PetType {
    GUNNER("Mitrailleur", NamedTextColor.RED, Color.fromRGB(140, 30, 30), Material.CROSSBOW, Material.AIR),
    MEDIC("Medic", NamedTextColor.GREEN, Color.fromRGB(60, 200, 90), Material.SPLASH_POTION, Material.GOLDEN_APPLE),
    SNIPER("Tireur d'elite", NamedTextColor.GRAY, Color.fromRGB(80, 80, 90), Material.BOW, Material.AIR),
    SCOUT("Eclaireur", NamedTextColor.YELLOW, Color.fromRGB(220, 200, 40), Material.CROSSBOW, Material.FEATHER),
    BRUTE("Brute", NamedTextColor.DARK_RED, Color.fromRGB(90, 20, 20), Material.IRON_SWORD, Material.AIR);

    private final String displayFr;
    private final NamedTextColor nameColor;
    private final Color leatherTint;
    private final Material main;
    private final Material off;

    PetType(String displayFr, NamedTextColor nameColor, Color leatherTint, Material main, Material off) {
        this.displayFr = displayFr;
        this.nameColor = nameColor;
        this.leatherTint = leatherTint;
        this.main = main;
        this.off = off;
    }

    public String displayFr() {
        return displayFr;
    }

    public NamedTextColor nameColor() {
        return nameColor;
    }

    public ItemStack createMainHand() {
        return new ItemStack(main);
    }

    public ItemStack createOffHand() {
        return off == Material.AIR ? null : new ItemStack(off);
    }

    public ItemStack leatherPiece(Material piece) {
        ItemStack stack = new ItemStack(piece);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(leatherTint);
        stack.setItemMeta(meta);
        return stack;
    }

    public static PetType fromArg(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "gunner", "mitrailleur" -> GUNNER;
            case "medic", "medecin" -> MEDIC;
            case "sniper" -> SNIPER;
            case "scout", "eclaireur" -> SCOUT;
            case "brute" -> BRUTE;
            default -> null;
        };
    }
}
