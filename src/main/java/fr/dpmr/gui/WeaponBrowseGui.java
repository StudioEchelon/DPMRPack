package fr.dpmr.gui;

import fr.dpmr.game.WeaponManager;
import fr.dpmr.game.WeaponProfile;
import fr.dpmr.game.WeaponRarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Menu par rarete : hub puis jusqu'a 15 armes par page.
 */
public class WeaponBrowseGui implements Listener {

    private static final Component TITLE_HUB = Component.text()
            .append(Component.text("◆ ", TextColor.color(0xF5C26B)))
            .append(Component.text("Catalogue", TextColor.color(0xFFF5E6), TextDecoration.BOLD))
            .append(Component.text(" DPMR", TextColor.color(0xB8B8C8)))
            .append(Component.text(" ◆", TextColor.color(0xF5C26B)))
            .build();

    private static final int SLOT_BACK = 49;
    private static final int[] HUB_RARITY_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int BUY_SHOWCASE_SLOT = 22;
    private static final int[] WEAPON_SLOTS = {
            10, 11, 12, 13, 14,
            19, 20, 21, 23, 24,
            28, 29, 30, 31, 32
    };

    private final WeaponManager weaponManager;

    public WeaponBrowseGui(WeaponManager weaponManager) {
        this.weaponManager = weaponManager;
    }

    public void openHub(Player player) {
        Inventory inv = WeaponCatalogHolder.create(54, TITLE_HUB, WeaponCatalogHolder.Kind.HUB, null);
        WeaponRarity[] order = WeaponRarity.values();
        for (int i = 0; i < order.length && i < HUB_RARITY_SLOTS.length; i++) {
            inv.setItem(HUB_RARITY_SLOTS[i], BoutiqueUi.hubRarityButton(order[i], BoutiqueUi.countWeapons(order[i])));
        }
        inv.setItem(SLOT_BACK, closeItem());
        BoutiqueUi.fillBackdrop(inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
    }

    public void openRarityPage(Player player, WeaponRarity rarity) {
        Inventory inv = WeaponCatalogHolder.create(54,
                Component.text("DPMR — ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(rarity.displayFr(), rarity.color(), TextDecoration.BOLD)),
                WeaponCatalogHolder.Kind.BROWSE_CATEGORY, rarity);
        List<WeaponProfile> list = new ArrayList<>();
        for (WeaponProfile w : WeaponProfile.values()) {
            if (w.rarity() == rarity) {
                list.add(w);
            }
        }
        list.sort(Comparator.comparing(Enum::name));
        int n = Math.min(WEAPON_SLOTS.length, list.size());
        for (int i = 0; i < n; i++) {
            ItemStack gun = weaponManager.createWeaponItem(list.get(i).name(), player);
            if (gun != null) {
                inv.setItem(WEAPON_SLOTS[i], gun);
            }
        }
        inv.setItem(BUY_SHOWCASE_SLOT, BoutiqueUi.categoryShowcase(rarity, list.size()));
        inv.setItem(45, backToHub());
        inv.setItem(SLOT_BACK, closeItem());
        BoutiqueUi.fillBackdrop(inv);
        player.openInventory(inv);
    }

    private static ItemStack backToHub() {
        ItemStack i = new ItemStack(Material.ARROW);
        ItemMeta m = i.getItemMeta();
        m.displayName(Component.text("← Retour categories", NamedTextColor.YELLOW));
        i.setItemMeta(m);
        return i;
    }

    private static ItemStack closeItem() {
        ItemStack i = new ItemStack(Material.BARRIER);
        ItemMeta m = i.getItemMeta();
        m.displayName(Component.text("Fermer", NamedTextColor.RED, TextDecoration.BOLD));
        i.setItemMeta(m);
        return i;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (WeaponCatalogHolder.from(event.getView().getTopInventory()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        WeaponCatalogHolder cat = WeaponCatalogHolder.from(event.getView().getTopInventory());
        if (cat == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        int slot = event.getRawSlot();
        ItemStack cur = event.getCurrentItem();
        if (cur == null || cur.getType().isAir()) {
            return;
        }

        if (cat.kind() == WeaponCatalogHolder.Kind.HUB) {
            if (slot == SLOT_BACK) {
                player.closeInventory();
                return;
            }
            WeaponRarity[] order = WeaponRarity.values();
            for (int i = 0; i < HUB_RARITY_SLOTS.length && i < order.length; i++) {
                if (slot == HUB_RARITY_SLOTS[i]) {
                    openRarityPage(player, order[i]);
                    return;
                }
            }
            return;
        }

        if (cat.kind() == WeaponCatalogHolder.Kind.BROWSE_CATEGORY) {
            if (slot == BUY_SHOWCASE_SLOT) {
                return;
            }
            if (slot == 45) {
                openHub(player);
                return;
            }
            if (slot == SLOT_BACK) {
                player.closeInventory();
            }
        }
    }
}
