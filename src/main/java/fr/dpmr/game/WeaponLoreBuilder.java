package fr.dpmr.game;

import fr.dpmr.i18n.GameLocale;
import fr.dpmr.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Localized weapon tooltips with combat stats (damage, RoF, DPS, mag, reload).
 */
public final class WeaponLoreBuilder {

    private WeaponLoreBuilder() {
    }

    public static void apply(ItemMeta meta, WeaponProfile w,
                            WeaponUpgradeState st, BombUpgradeState bombSt, JerrycanUpgradeState jerrySt,
                            GameLocale loc) {
        meta.displayName(Component.text(I18n.string(loc, "weapon.prefix") + " " + w.displayName(), w.color()));
        List<Component> lore = new ArrayList<>();
        boolean boldRare = w.rarity() == WeaponRarity.LEGENDARY || w.rarity() == WeaponRarity.MYTHIC
                || w.rarity() == WeaponRarity.GHOST;
        String rar = I18n.string(loc, "rarity." + w.rarity().name());
        lore.add(boldRare
                ? Component.text(rar, w.rarity().color(), TextDecoration.BOLD)
                : Component.text(rar, w.rarity().color()));

        if (w == WeaponProfile.JERRYCAN) {
            lore.add(I18n.component(loc, NamedTextColor.YELLOW, "weapon.jerry_line1"));
            lore.add(I18n.component(loc, NamedTextColor.GOLD, "weapon.jerry_line2"));
        } else if (w.isGrapplingHook()) {
            lore.add(Component.text(I18n.string(loc, "firemode.GRAPPLE_BEAM") + " — " + I18n.string(loc, "weapon.grapple_line1"), NamedTextColor.AQUA));
            lore.add(I18n.component(loc, NamedTextColor.DARK_GRAY, "weapon.grapple_line2"));
            lore.add(I18n.component(loc, NamedTextColor.GRAY, "weapon.stat_range", (int) w.baseRange()));
        } else if (w.fireMode() == FireMode.PROJECTILE_SERUM_ZONE) {
            lore.add(Component.text(I18n.string(loc, "firemode.PROJECTILE_SERUM_ZONE") + " — " + I18n.string(loc, "weapon.serum_line1"), NamedTextColor.GREEN));
            lore.add(I18n.component(loc, NamedTextColor.DARK_AQUA, "weapon.serum_line2",
                    String.format(Locale.ROOT, "%.1f", w.baseRange())));
            lore.add(I18n.component(loc, NamedTextColor.GRAY, "weapon.serum_line3"));
        } else if (w.fireMode() == FireMode.PROJECTILE_HEAL_DART) {
            lore.add(Component.text(I18n.string(loc, "firemode.PROJECTILE_HEAL_DART") + " — " + I18n.string(loc, "weapon.heal_dart_line1"), NamedTextColor.LIGHT_PURPLE));
            lore.add(I18n.component(loc, NamedTextColor.DARK_PURPLE, "weapon.heal_dart_line2",
                    String.format(Locale.ROOT, "%.1f", w.baseDamage()),
                    String.format(Locale.ROOT, "%.1f", w.baseDamage() / 2.0)));
            int cdTicks = Math.max(1, w.cooldownTicks());
            double rof = 20.0 / cdTicks;
            lore.add(I18n.component(loc, NamedTextColor.GRAY, "weapon.stat_fire_rate", String.format(Locale.ROOT, "%.2f", rof)));
        } else if (w.isNuclearWeapon()) {
            lore.add(Component.text(I18n.string(loc, "firemode.NUCLEAR_STRIKE") + " — " + I18n.string(loc, "weapon.nuke_line1"), NamedTextColor.LIGHT_PURPLE));
        } else {
            appendCombatStats(lore, w, loc, jerrySt);
        }

        if (w.hasScope()) {
            lore.add(I18n.component(loc, NamedTextColor.AQUA, "weapon.scope_line"));
        }
        if (w.hasHeavyWeight()) {
            lore.add(I18n.component(loc, NamedTextColor.DARK_RED, "weapon.heavy_line", w.heavyHoldSlowAmplifier() + 1));
        }
        if (w == WeaponProfile.LANCE_MARRONS) {
            lore.add(I18n.component(loc, NamedTextColor.GOLD, "weapon.chestnut_extra"));
        }
        if (w.isBombWeapon()) {
            lore.add(I18n.component(loc, NamedTextColor.RED, "weapon.bomb_table"));
        }

        for (Component line : BombUpgradeEffects.loreLines(bombSt, loc)) {
            lore.add(line);
        }
        for (Component line : JerrycanUpgradeEffects.loreLines(jerrySt, loc)) {
            lore.add(line);
        }

        int cap = w.clipSize() + (w == WeaponProfile.JERRYCAN ? JerrycanUpgradeEffects.clipBonus(jerrySt) : 0);
        if (showsMagLine(w)) {
            lore.add(I18n.component(loc, NamedTextColor.GRAY, "weapon.stat_mag", cap));
        }
        if (showsAmmoNote(w)) {
            lore.add(I18n.component(loc, NamedTextColor.DARK_GRAY, "weapon.ammo_note"));
        }
        lore.add(I18n.component(loc, NamedTextColor.DARK_GRAY, "weapon.controls"));

        for (Component line : WeaponUpgradeEffects.loreUpgradeLines(st, loc)) {
            lore.add(line);
        }
        meta.lore(lore);
    }

    private static String modeLine(WeaponProfile w, GameLocale loc) {
        String mode = I18n.string(loc, "firemode." + w.fireMode().name());
        if (w.isGrapplingHook()) {
            return mode;
        }
        if (w.isNuclearWeapon()) {
            return mode;
        }
        if (w.fireMode() == FireMode.PROJECTILE_SERUM_ZONE || w.fireMode() == FireMode.PROJECTILE_HEAL_DART) {
            return mode;
        }
        return mode + " | " + I18n.string(loc, "weapon.stat_damage_total",
                String.format(Locale.ROOT, "%.1f", w.baseDamage()),
                String.format(Locale.ROOT, "%.1f", w.baseDamage() / 2.0));
    }

    private static void appendCombatStats(List<Component> lore, WeaponProfile w, GameLocale loc, JerrycanUpgradeState jerrySt) {
        lore.add(Component.text(modeLine(w, loc), NamedTextColor.GRAY));

        if (w.pellets() > 1) {
            double per = w.baseDamage() / w.pellets();
            lore.add(I18n.component(loc, NamedTextColor.GRAY, "weapon.stat_damage_pellet",
                    w.pellets(),
                    String.format(Locale.ROOT, "%.2f", per),
                    String.format(Locale.ROOT, "%.1f", w.baseDamage())));
        }

        int cdTicks = Math.max(1, w.cooldownTicks());
        double cdSec = cdTicks / 20.0;
        double rof = 1.0 / cdSec;
        double dmg = w.baseDamage();
        double burstDps = dmg * (20.0 / cdTicks);
        lore.add(I18n.component(loc, NamedTextColor.GRAY, "weapon.stat_fire_rate", String.format(Locale.ROOT, "%.2f", rof)));
        lore.add(I18n.component(loc, NamedTextColor.GREEN, "weapon.stat_burst_dps",
                String.format(Locale.ROOT, "%.1f", burstDps),
                String.format(Locale.ROOT, "%.1f", burstDps / 2.0)));

        double reloadSec = w.reloadTicks() / 20.0;
        lore.add(I18n.component(loc, NamedTextColor.GRAY, "weapon.stat_reload_time",
                String.format(Locale.ROOT, "%.2f", reloadSec)));

        int clip = w.clipSize() + (w == WeaponProfile.JERRYCAN ? JerrycanUpgradeEffects.clipBonus(jerrySt) : 0);
        double cycleTicks = (long) clip * cdTicks + w.reloadTicks();
        if (cycleTicks > 0 && clip > 0) {
            double sustained = (clip * dmg) / (cycleTicks / 20.0);
            lore.add(I18n.component(loc, NamedTextColor.DARK_GREEN, "weapon.stat_sustained_dps",
                    String.format(Locale.ROOT, "%.1f", sustained)));
        }

        lore.add(I18n.component(loc, NamedTextColor.DARK_GRAY, "weapon.stat_range", (int) w.baseRange()));
        lore.add(I18n.component(loc, NamedTextColor.DARK_GRAY, "weapon.stat_spread",
                String.format(Locale.ROOT, "%.1f", w.baseSpreadDegrees())));
    }

    private static boolean showsMagLine(WeaponProfile w) {
        if (w.isGrapplingHook() || w.isNuclearWeapon()) {
            return false;
        }
        if (w.fireMode() == FireMode.PROJECTILE_SERUM_ZONE || w.fireMode() == FireMode.PROJECTILE_GASOLINE) {
            return false;
        }
        return w != WeaponProfile.JERRYCAN || w.clipSize() > 0;
    }

    private static boolean showsAmmoNote(WeaponProfile w) {
        if (w.isGrapplingHook() || w.isNuclearWeapon()) {
            return false;
        }
        return w.fireMode() != FireMode.PROJECTILE_SERUM_ZONE;
    }
}
