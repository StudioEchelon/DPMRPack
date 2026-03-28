package fr.dpmr.game;

import fr.dpmr.i18n.GameLocale;
import fr.dpmr.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Effets gameplay par voie / palier (une seule voie active).
 */
public final class WeaponUpgradeEffects {

    private WeaponUpgradeEffects() {
    }

    public static int goldCostForTier(int tier) {
        return tier * 2;
    }

    public static String tierTitle(WeaponUpgradePath path, int tier) {
        return tierTitle(path, tier, GameLocale.FR);
    }

    public static String tierTitle(WeaponUpgradePath path, int tier, GameLocale loc) {
        if (loc == GameLocale.EN) {
            return switch (path) {
                case ASSAULT -> switch (tier) {
                    case 1 -> "I — Heavy caliber";
                    case 2 -> "II — Burst";
                    case 3 -> "III — Ricochet";
                    case 4 -> "IV — Critical hit";
                    case 5 -> "V — Explosive volley";
                    default -> "?";
                };
                case SURVIVAL -> switch (tier) {
                    case 1 -> "I — Watertight pouches";
                    case 2 -> "II — Fast magazine";
                    case 3 -> "III — Siphon";
                    case 4 -> "IV — Reactive plating";
                    case 5 -> "V — Free bullet";
                    default -> "?";
                };
                case TECH -> switch (tier) {
                    case 1 -> "I — Stabilizer";
                    case 2 -> "II — Long sight";
                    case 3 -> "III — Conductive cold";
                    case 4 -> "IV — Voltaic arc";
                    case 5 -> "V — Marked shot";
                    default -> "?";
                };
            };
        }
        return switch (path) {
            case ASSAULT -> switch (tier) {
                case 1 -> "I — Calibre lourd";
                case 2 -> "II — Rafale";
                case 3 -> "III — Ricochet";
                case 4 -> "IV — Coup critique";
                case 5 -> "V — Salve explosive";
                default -> "?";
            };
            case SURVIVAL -> switch (tier) {
                case 1 -> "I — Sacs etanches";
                case 2 -> "II — Chargeur rapide";
                case 3 -> "III — Siphon";
                case 4 -> "IV — Plaque reactive";
                case 5 -> "V — Munition gratuite";
                default -> "?";
            };
            case TECH -> switch (tier) {
                case 1 -> "I — Stabilisateur";
                case 2 -> "II — Longue vue";
                case 3 -> "III — Gel conducteur";
                case 4 -> "IV — Arc voltaïque";
                case 5 -> "V — Tir marque";
                default -> "?";
            };
        };
    }

    public static List<String> tierDescriptionLines(WeaponUpgradePath path, int tier) {
        return switch (path) {
            case ASSAULT -> switch (tier) {
                case 1 -> List.of("+10% degats");
                case 2 -> List.of("-15% temps entre les tirs");
                case 3 -> List.of("Ricochet: 2e cible proche", "35% des degats");
                case 4 -> List.of("12% chance x1.35 degats");
                case 5 -> List.of("12% mini-explosion", "zone ~2.5 blocs");
                default -> List.of();
            };
            case SURVIVAL -> switch (tier) {
                case 1 -> List.of("+18% munitions reserve");
                case 2 -> List.of("-20% duree reload");
                case 3 -> List.of("Vol de vie: 7% des degats");
                case 4 -> List.of("15% Absorption I (3s)");
                case 5 -> List.of("11% ne consomme pas", "la balle");
                default -> List.of();
            };
            case TECH -> switch (tier) {
                case 1 -> List.of("-14% dispersion");
                case 2 -> List.of("+12% portee");
                case 3 -> List.of("Lenteur I 1s sur la cible");
                case 4 -> List.of("18% chaine proche", "15% degats");
                case 5 -> List.of("Tous les 6 tirs:", "+22% degats + lueur");
                default -> List.of();
            };
        };
    }

    public static double damageMultiplier(ItemStack weapon, WeaponUpgradeState st) {
        if (st.path() != WeaponUpgradePath.ASSAULT) {
            return 1.0;
        }
        return 1.0 + 0.10 * countTier(st, 1);
    }

    public static double cooldownMultiplier(ItemStack weapon, WeaponUpgradeState st) {
        if (st.path() != WeaponUpgradePath.ASSAULT) {
            return 1.0;
        }
        double m = 1.0;
        if (st.tier() >= 2) {
            m *= 0.85;
        }
        return m;
    }

    public static double reloadMultiplier(ItemStack weapon, WeaponUpgradeState st) {
        if (st.path() != WeaponUpgradePath.SURVIVAL) {
            return 1.0;
        }
        double m = 1.0;
        if (st.tier() >= 2) {
            m *= 0.80;
        }
        return m;
    }

    public static double spreadMultiplier(ItemStack weapon, WeaponUpgradeState st) {
        if (st.path() != WeaponUpgradePath.TECH) {
            return 1.0;
        }
        double m = 1.0;
        if (st.tier() >= 1) {
            m *= 0.86;
        }
        return m;
    }

    public static double rangeMultiplier(ItemStack weapon, WeaponUpgradeState st) {
        if (st.path() != WeaponUpgradePath.TECH) {
            return 1.0;
        }
        double m = 1.0;
        if (st.tier() >= 2) {
            m *= 1.12;
        }
        return m;
    }

    public static int extraReserveSlots(WeaponProfile profile, WeaponUpgradeState st) {
        if (st.path() != WeaponUpgradePath.SURVIVAL || st.tier() < 1) {
            return 0;
        }
        return (int) Math.round(profile.reserveAmmo() * 0.18);
    }

    private static int countTier(WeaponUpgradeState st, int minTier) {
        if (st.path() == null) {
            return 0;
        }
        return st.tier() >= minTier ? 1 : 0;
    }

    public static List<Component> loreUpgradeLines(WeaponUpgradeState st) {
        return loreUpgradeLines(st, GameLocale.FR);
    }

    public static List<Component> loreUpgradeLines(WeaponUpgradeState st, GameLocale loc) {
        List<Component> lines = new ArrayList<>();
        if (st.isEmpty()) {
            return lines;
        }
        WeaponUpgradePath path = st.path();
        lines.add(Component.text(I18n.string(loc, "upgrade.path_line",
                path.styleName(loc), path.shortLabel(loc)), NamedTextColor.LIGHT_PURPLE));
        lines.add(Component.text(I18n.string(loc, "upgrade.tier_summary", st.tier(), path.blurb(loc)), NamedTextColor.DARK_GRAY));
        for (int t = 1; t <= st.tier(); t++) {
            lines.add(Component.text("  " + tierTitle(path, t, loc), NamedTextColor.AQUA));
        }
        return lines;
    }

    public static boolean canBuyTier(WeaponUpgradeState current, WeaponUpgradePath path, int tier) {
        if (tier < 1 || tier > 5) {
            return false;
        }
        if (current.isEmpty()) {
            return tier == 1;
        }
        if (current.path() != path) {
            return false;
        }
        return tier == current.tier() + 1;
    }

    public static boolean pathLockedForDisplay(WeaponUpgradeState current, WeaponUpgradePath path) {
        return !current.isEmpty() && current.path() != path;
    }

    public static boolean tierAlreadyOwned(WeaponUpgradeState current, WeaponUpgradePath path, int tier) {
        return !current.isEmpty() && current.path() == path && current.tier() >= tier;
    }
}
