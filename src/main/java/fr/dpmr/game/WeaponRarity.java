package fr.dpmr.game;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum WeaponRarity {
    COMMON(NamedTextColor.GRAY, "Commun"),
    UNCOMMON(NamedTextColor.GREEN, "Peu commun"),
    RARE(NamedTextColor.AQUA, "Rare"),
    EPIC(NamedTextColor.LIGHT_PURPLE, "Epique"),
    LEGENDARY(NamedTextColor.GOLD, "Legendaire"),
    MYTHIC(TextColor.color(0xFF3D9A), "Mythique"),
    GHOST(TextColor.color(0x9B6BFF), "Ghost");

    private final TextColor color;
    private final String displayFr;

    WeaponRarity(TextColor color, String displayFr) {
        this.color = color;
        this.displayFr = displayFr;
    }

    public TextColor color() {
        return color;
    }

    public String displayFr() {
        return displayFr;
    }

    public boolean glint() {
        return this == RARE || this == EPIC || this == LEGENDARY || this == MYTHIC || this == GHOST;
    }
}
