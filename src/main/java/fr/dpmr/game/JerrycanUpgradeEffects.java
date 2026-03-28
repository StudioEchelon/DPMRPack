package fr.dpmr.game;

import fr.dpmr.i18n.GameLocale;
import fr.dpmr.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class JerrycanUpgradeEffects {

    private JerrycanUpgradeEffects() {
    }

    public static int goldCostForTier(int tier) {
        return tier * 3;
    }

    public static String tierTitle(JerrycanUpgradePath path, int tier) {
        return tierTitle(path, tier, GameLocale.FR);
    }

    public static String tierTitle(JerrycanUpgradePath path, int tier, GameLocale loc) {
        if (loc == GameLocale.EN) {
            return switch (path) {
                case THERMAL -> switch (tier) {
                    case 1 -> "I — Enriched blend";
                    case 2 -> "II — Magnesium additive";
                    case 3 -> "III — Pressurized tank";
                    case 4 -> "IV — Ground fire sheet";
                    case 5 -> "V — WHITE INFERNO";
                    default -> "?";
                };
                case VISCOUS -> switch (tier) {
                    case 1 -> "I — Artisan tar";
                    case 2 -> "II — Slow burn";
                    case 3 -> "III — Recycled napalm";
                    case 4 -> "IV — Military gelatin";
                    case 5 -> "V — THE BLACK GRIP";
                    default -> "?";
                };
                case BREACH -> switch (tier) {
                    case 1 -> "I — Soft pin";
                    case 2 -> "II — Fragile shell";
                    case 3 -> "III — Double charge";
                    case 4 -> "IV — Toxic vapors";
                    case 5 -> "V — FUEL RAIN";
                    default -> "?";
                };
            };
        }
        return switch (path) {
            case THERMAL -> switch (tier) {
                case 1 -> "I — Melange enrichi";
                case 2 -> "II — Additif magnesium";
                case 3 -> "III — Reservoir pressurise";
                case 4 -> "IV — Nappe de feu";
                case 5 -> "V — ENFER BLANC";
                default -> "?";
            };
            case VISCOUS -> switch (tier) {
                case 1 -> "I — Goudron artisanal";
                case 2 -> "II — Combustion lente";
                case 3 -> "III — Napalm recycle";
                case 4 -> "IV — Gelatine militaire";
                case 5 -> "V — L'ETREINTE NOIRE";
                default -> "?";
            };
            case BREACH -> switch (tier) {
                case 1 -> "I — Goupille souple";
                case 2 -> "II — Coque fragile";
                case 3 -> "III — Double charge";
                case 4 -> "IV — Vapeurs toxiques";
                case 5 -> "V — PLUIE DE COMBUSTIBLE";
                default -> "?";
            };
        };
    }

    public static List<String> tierLines(JerrycanUpgradePath path, int tier) {
        return switch (path) {
            case THERMAL -> switch (tier) {
                case 1 -> List.of("Rayon de brulure +10%");
                case 2 -> List.of("Degats de brulure +15%");
                case 3 -> List.of("Portee du lancer +25%");
                case 4 -> List.of("Le feu se propage sur ~3 blocs au sol");
                case 5 -> List.of("Degats doubles, ignore la resistance au feu");
                default -> List.of();
            };
            case VISCOUS -> switch (tier) {
                case 1 -> List.of("Ralentissement ~10% dans la zone");
                case 2 -> List.of("Duree du feu +3 s");
                case 3 -> List.of("Ralentissement cumulable (jusqu'a 3 stacks)");
                case 4 -> List.of("Les ennemis restent en feu hors de la flaque");
                case 5 -> List.of("Ralentissement extreme, plus de sprint");
                default -> List.of();
            };
            case BREACH -> switch (tier) {
                case 1 -> List.of("Lancer plus rapide (-18% cooldown)");
                case 2 -> List.of("Explosion instantanee a l'impact (pas de flaque)");
                case 3 -> List.of("Capacite du chargeur +1");
                case 4 -> List.of("Nuage de fumee aveuglante avant l'explosion");
                case 5 -> List.of("3 mini-jerricans par tir");
                default -> List.of();
            };
        };
    }

    public static boolean canBuyTier(JerrycanUpgradeState current, JerrycanUpgradePath path, int tier) {
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

    public static boolean pathLocked(JerrycanUpgradeState current, JerrycanUpgradePath path) {
        return !current.isEmpty() && current.path() != path;
    }

    public static boolean tierOwned(JerrycanUpgradeState current, JerrycanUpgradePath path, int tier) {
        return !current.isEmpty() && current.path() == path && current.tier() >= tier;
    }

    public static int clipBonus(JerrycanUpgradeState st) {
        if (st.isEmpty() || st.path() != JerrycanUpgradePath.BREACH || st.tier() < 3) {
            return 0;
        }
        return 1;
    }

    /** Multiplicateur sur le rayon des degats / entites enflammees. */
    public static double igniteRadiusMul(JerrycanUpgradeState st) {
        if (st.path() == JerrycanUpgradePath.THERMAL && st.tier() >= 1) {
            return 1.1;
        }
        return 1.0;
    }

    public static double burnDamageMul(JerrycanUpgradeState st) {
        double m = 1.0;
        if (st.path() == JerrycanUpgradePath.THERMAL) {
            if (st.tier() >= 2) {
                m *= 1.15;
            }
            if (st.tier() >= 5) {
                m *= 2.0;
            }
        }
        return m;
    }

    public static boolean infernoBlanc(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.THERMAL && st.tier() >= 5;
    }

    public static double throwVelocityMul(JerrycanUpgradeState st) {
        if (st.path() == JerrycanUpgradePath.THERMAL && st.tier() >= 3) {
            return 1.25;
        }
        return 1.0;
    }

    public static boolean wideGroundFire(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.THERMAL && st.tier() >= 4;
    }

    public static int extraFireTicks(JerrycanUpgradeState st) {
        if (st.path() == JerrycanUpgradePath.VISCOUS && st.tier() >= 2) {
            return 60;
        }
        return 0;
    }

    public static boolean viscousSlow(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.VISCOUS && st.tier() >= 1;
    }

    public static boolean stackingSlow(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.VISCOUS && st.tier() >= 3;
    }

    public static boolean firePersistsOffPuddle(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.VISCOUS && st.tier() >= 4;
    }

    public static boolean etreinteNoire(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.VISCOUS && st.tier() >= 5;
    }

    public static double jerryCooldownMul(JerrycanUpgradeState st) {
        if (st.path() == JerrycanUpgradePath.BREACH && st.tier() >= 1) {
            return 0.82;
        }
        return 1.0;
    }

    public static boolean instantImpact(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.BREACH && st.tier() >= 2;
    }

    public static boolean smokeCloud(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.BREACH && st.tier() >= 4;
    }

    public static boolean tripleVolley(JerrycanUpgradeState st) {
        return st.path() == JerrycanUpgradePath.BREACH && st.tier() >= 5;
    }

    public static List<Component> loreLines(JerrycanUpgradeState st) {
        return loreLines(st, GameLocale.FR);
    }

    public static List<Component> loreLines(JerrycanUpgradeState st, GameLocale loc) {
        List<Component> lines = new ArrayList<>();
        if (st.isEmpty()) {
            return lines;
        }
        lines.add(Component.text(I18n.string(loc, "upgrade.jerry_header", st.path().styleName(loc)), NamedTextColor.GOLD));
        for (int t = 1; t <= st.tier(); t++) {
            lines.add(Component.text("  " + tierTitle(st.path(), t, loc), NamedTextColor.YELLOW));
        }
        return lines;
    }
}
