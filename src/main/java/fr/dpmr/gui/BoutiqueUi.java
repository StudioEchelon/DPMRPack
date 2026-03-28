package fr.dpmr.gui;

import fr.dpmr.game.WeaponProfile;
import fr.dpmr.game.WeaponRarity;
import fr.dpmr.npc.NpcSkins;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Icones tete joueur et style commun pour la boutique DPMR.
 */
public final class BoutiqueUi {

    private BoutiqueUi() {
    }

    public static int countWeapons(WeaponRarity rarity) {
        int n = 0;
        for (WeaponProfile w : WeaponProfile.values()) {
            if (w.rarity() == rarity) {
                n++;
            }
        }
        return n;
    }

    public static Component titleHub() {
        return Component.text()
                .append(Component.text("◆ ", TextColor.color(0xF5C26B)))
                .append(Component.text("BOUTIQUE", TextColor.color(0xFFF5E6), TextDecoration.BOLD))
                .append(Component.text(" DPMR", TextColor.color(0xB8B8C8)))
                .append(Component.text(" ◆", TextColor.color(0xF5C26B)))
                .build();
    }

    public static Component titleBuyCategory(WeaponRarity rarity) {
        return Component.text()
                .append(Component.text("Acheter ", NamedTextColor.DARK_GRAY))
                .append(Component.text("· ", TextColor.color(0x5A5A68)))
                .append(Component.text(rarity.displayFr(), rarity.color(), TextDecoration.BOLD))
                .build();
    }

    public static Component titleSell() {
        return Component.text()
                .append(Component.text("Reprise ", TextColor.color(0xE8C170), TextDecoration.BOLD))
                .append(Component.text("· armes", NamedTextColor.DARK_GRAY))
                .build();
    }

    public static ItemStack rarityPortrait(WeaponRarity rarity) {
        String hash = switch (rarity) {
            case COMMON -> "1000cfb2ff1a984c3b33f72a25f49dff16839650bed677073d2d4528efd8eabe";
            case UNCOMMON -> "100191e52d207a0ef4972ff8393e4ed1277b1b872e72e7830aff09e938f337ec";
            case RARE -> "1003701ba9a2ed6364ac2f513b357e7472d207447f889be695eaf3590abfb037";
            case EPIC -> "10073479cfcfe3c1a592b903e783af33e8ae1d3d928a9f92febce4af5419f5b9";
            case LEGENDARY -> "13a6c9d854e84902b5e8b85faedcda975d1dcbb0803734274a172edc25e9316d";
            case MYTHIC -> "13b906faedc09c9f10fe17478a282d15bdb310bd512be345e9eb182ab6f210b5";
            case GHOST -> "1002f90ba0b230f6e9b22f163ec99a93da45c7f148f93d234652b507f6dbb374";
        };
        return NpcSkins.playerHeadFromTextureHash(hash, "dpmr-boutique-rarity-" + rarity.name());
    }

    public static ItemStack hubRarityButton(WeaponRarity rarity, int weaponCount) {
        ItemStack sk = rarityPortrait(rarity);
        ItemMeta m = sk.getItemMeta();
        m.displayName(Component.text(rarity.displayFr(), rarity.color(), TextDecoration.BOLD));
        m.lore(List.of(
                Component.text(weaponCount + " arme" + (weaponCount > 1 ? "s" : ""), NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Cliquer pour parcourir", TextColor.color(0x8A8A98))
        ));
        sk.setItemMeta(m);
        return sk;
    }

    public static ItemStack pane(Material material) {
        ItemStack p = new ItemStack(material);
        ItemMeta m = p.getItemMeta();
        m.displayName(Component.text(" "));
        p.setItemMeta(m);
        return p;
    }

    public static boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }

    public static void fillBackdrop(Inventory inv) {
        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack inner = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int s = 0; s < inv.getSize(); s++) {
            if (inv.getItem(s) != null) {
                continue;
            }
            inv.setItem(s, isBorderSlot(s) ? border.clone() : inner.clone());
        }
    }

    public static ItemStack categoryShowcase(WeaponRarity rarity, int weaponCount) {
        ItemStack sk = rarityPortrait(rarity);
        ItemMeta m = sk.getItemMeta();
        m.displayName(Component.text("Rayon ", NamedTextColor.DARK_GRAY)
                .append(Component.text(rarity.displayFr(), rarity.color(), TextDecoration.BOLD)));
        m.lore(List.of(
                Component.text(weaponCount + " modele" + (weaponCount > 1 ? "s" : "") + " disponible"
                        + (weaponCount > 1 ? "s" : ""), NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Paiement en points DPMR", TextColor.color(0xC9A86A))
        ));
        sk.setItemMeta(m);
        return sk;
    }
}
