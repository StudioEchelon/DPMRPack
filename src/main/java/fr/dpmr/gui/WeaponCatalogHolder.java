package fr.dpmr.gui;

import fr.dpmr.game.WeaponRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Identifie les inventaires du catalogue /armes sans se fier au titre Adventure.
 */
public final class WeaponCatalogHolder implements InventoryHolder {

    public enum Kind {
        HUB,
        BROWSE_CATEGORY
    }

    private Inventory inventory;
    private final Kind kind;
    /** Non-null seulement si {@link Kind#BROWSE_CATEGORY}. */
    private final WeaponRarity browseCategory;

    private WeaponCatalogHolder(Kind kind, WeaponRarity browseCategory) {
        this.kind = kind;
        this.browseCategory = browseCategory;
    }

    public Kind kind() {
        return kind;
    }

    /** Defini seulement pour {@link Kind#BROWSE_CATEGORY}. */
    public WeaponRarity browseCategory() {
        return browseCategory;
    }

    void attach(Inventory inv) {
        this.inventory = inv;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static Inventory create(int size, Component title, Kind kind, WeaponRarity browseCategoryOrNull) {
        if (kind == Kind.BROWSE_CATEGORY && browseCategoryOrNull == null) {
            throw new IllegalArgumentException("browseCategoryOrNull requis pour BROWSE_CATEGORY");
        }
        WeaponCatalogHolder holder = new WeaponCatalogHolder(kind,
                kind == Kind.HUB ? null : browseCategoryOrNull);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.attach(inv);
        return inv;
    }

    public static WeaponCatalogHolder from(Inventory top) {
        if (top == null) {
            return null;
        }
        InventoryHolder raw = top.getHolder(false);
        if (raw instanceof WeaponCatalogHolder h) {
            return h;
        }
        return null;
    }
}
