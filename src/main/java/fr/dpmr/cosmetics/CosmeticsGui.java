package fr.dpmr.cosmetics;

import fr.dpmr.data.PointsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
import java.util.List;
import java.util.Locale;

public class CosmeticsGui implements Listener {

    public static final Component TITLE = Component.text("Cosmetics DPMR", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
    private static final int SLOT_CLOSE = 49;

    private final CosmeticsManager cosmeticsManager;
    private final PointsManager pointsManager;

    public CosmeticsGui(CosmeticsManager cosmeticsManager, PointsManager pointsManager) {
        this.cosmeticsManager = cosmeticsManager;
        this.pointsManager = pointsManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        int slot = 10;
        for (CosmeticProfile p : CosmeticProfile.values()) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot >= 45) break;
            inv.setItem(slot++, entryItem(player, p));
        }
        inv.setItem(SLOT_CLOSE, closeItem());
        fillGlass(inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, 1.2f);
    }

    private ItemStack entryItem(Player player, CosmeticProfile p) {
        boolean owned = cosmeticsManager.isOwned(player.getUniqueId(), p);
        ItemStack i = new ItemStack(p.icon());
        ItemMeta m = i.getItemMeta();
        m.displayName(Component.text(p.displayName(), p.color()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Type: " + p.type().name().toLowerCase(Locale.ROOT), NamedTextColor.GRAY));
        lore.add(Component.text("Prix: " + p.price() + " points", NamedTextColor.GOLD));
        lore.add(Component.text(owned ? "Clic: equip" : "Clic: acheter", owned ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        m.lore(lore);
        i.setItemMeta(m);
        return i;
    }

    private static ItemStack closeItem() {
        ItemStack i = new ItemStack(Material.BARRIER);
        ItemMeta m = i.getItemMeta();
        m.displayName(Component.text("Fermer", NamedTextColor.RED));
        i.setItemMeta(m);
        return i;
    }

    private static void fillGlass(Inventory inv) {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = g.getItemMeta();
        m.displayName(Component.text(" "));
        g.setItemMeta(m);
        for (int s = 0; s < 54; s++) {
            if (inv.getItem(s) == null) {
                inv.setItem(s, g.clone());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (TITLE.equals(event.getView().title())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().title())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        ItemStack cur = event.getCurrentItem();
        if (cur == null || cur.getType().isAir() || cur.getItemMeta() == null || cur.getItemMeta().displayName() == null) {
            return;
        }

        CosmeticProfile picked = null;
        // detection simple via type+name (suffisant ici car icones uniques et liste fixe)
        for (CosmeticProfile p : CosmeticProfile.values()) {
            if (p.icon() == cur.getType()) {
                picked = p;
                break;
            }
        }
        if (picked == null) {
            return;
        }

        boolean owned = cosmeticsManager.isOwned(player.getUniqueId(), picked);
        if (!owned) {
            int pts = pointsManager.getPoints(player.getUniqueId());
            if (pts < picked.price()) {
                player.sendMessage(Component.text("Pas assez de points (" + pts + "/" + picked.price() + ").", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 0.85f);
                return;
            }
            cosmeticsManager.buy(player.getUniqueId(), picked);
            player.sendMessage(Component.text("Achete: " + picked.displayName() + " (-" + picked.price() + " pts)", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
        }

        cosmeticsManager.setSelected(player.getUniqueId(), picked);
        if (picked.type() == CosmeticType.VANITY) {
            cosmeticsManager.giveVanity(player, picked);
        } else {
            player.sendActionBar(Component.text("Equipe: " + picked.displayName(), NamedTextColor.GREEN));
        }
        open(player);
    }
}

