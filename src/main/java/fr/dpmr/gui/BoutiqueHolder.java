package fr.dpmr.gui;

import fr.dpmr.game.WeaponRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Identifie les inventaires boutique sans se fier au titre (serialisation Adventure / legacy).
 */
public final class BoutiqueHolder implements InventoryHolder {

    public enum Kind {
        HUB,
        SELL,
        BUY_CATEGORY
    }

    private Inventory inventory;
    private final Kind kind;
    /** Non-null uniquement si {@link Kind#BUY_CATEGORY}. */
    private final WeaponRarity buyCategory;

    private BoutiqueHolder(Kind kind, WeaponRarity buyCategory) {
        this.kind = kind;
        this.buyCategory = buyCategory;
    }

    public Kind kind() {
        return kind;
    }

    /** Defini seulement pour {@link Kind#BUY_CATEGORY}. */
    public WeaponRarity buyCategory() {
        return buyCategory;
    }

    void attach(Inventory inv) {
        this.inventory = inv;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static Inventory create(int size, Component title, Kind kind, WeaponRarity buyCategoryOrNull) {
        BoutiqueHolder holder = new BoutiqueHolder(kind, buyCategoryOrNull);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.attach(inv);
        return inv;
    }

    public static BoutiqueHolder from(Inventory top) {
        if (top == null) {
            return null;
        }
        InventoryHolder raw = top.getHolder(false);
        if (raw instanceof BoutiqueHolder h) {
            return h;
        }
        return null;
    }
}
