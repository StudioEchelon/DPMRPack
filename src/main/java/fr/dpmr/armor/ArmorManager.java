package fr.dpmr.armor;

import fr.dpmr.game.WeaponDamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ArmorManager {

    private final NamespacedKey keyArmorProfile;

    public ArmorManager(JavaPlugin plugin) {
        this.keyArmorProfile = new NamespacedKey(plugin, "dpmr_armor_profile");
    }

    public List<ItemStack> createArmorSet(ArmorProfile profile) {
        List<ItemStack> out = new ArrayList<>();
        out.add(piece(profile.helmetMaterial(), profile, "Casque"));
        out.add(piece(profile.chestMaterial(), profile, "Plastron"));
        out.add(piece(profile.legsMaterial(), profile, "Jambieres"));
        out.add(piece(profile.bootsMaterial(), profile, "Bottes"));
        return out;
    }

    /**
     * Une pièce d'armure DPMR aléatoire (profil + slot), pour loot coffre / caisses.
     */
    public ItemStack createRandomArmorPiece() {
        ArmorProfile[] profiles = ArmorProfile.values();
        ArmorProfile p = profiles[ThreadLocalRandom.current().nextInt(profiles.length)];
        return switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> piece(p.helmetMaterial(), p, "Casque");
            case 1 -> piece(p.chestMaterial(), p, "Plastron");
            case 2 -> piece(p.legsMaterial(), p, "Jambieres");
            default -> piece(p.bootsMaterial(), p, "Bottes");
        };
    }

    private ItemStack piece(org.bukkit.Material mat, ArmorProfile p, String slot) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(slot + " " + p.label(), p.color()));
        meta.lore(List.of(
                Component.text("Armure tactique", NamedTextColor.GRAY),
                Component.text("Role: " + p.label(), p.color())
        ));
        meta.getPersistentDataContainer().set(keyArmorProfile, PersistentDataType.STRING, p.name());
        it.setItemMeta(meta);
        return it;
    }

    public double damageMultiplier(Player target, WeaponDamageType type) {
        ArmorProfile profile = equippedProfile(target);
        if (profile == null) {
            return 1.0;
        }
        return profile.multiplier(type);
    }

    private ArmorProfile equippedProfile(Player target) {
        if (target.getInventory().getChestplate() == null || !target.getInventory().getChestplate().hasItemMeta()) {
            return null;
        }
        String raw = target.getInventory().getChestplate().getItemMeta().getPersistentDataContainer()
                .get(keyArmorProfile, PersistentDataType.STRING);
        return ArmorProfile.fromId(raw);
    }
}

