package fr.dpmr.game;

import fr.dpmr.i18n.GameLocale;
import fr.dpmr.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class BombUpgradeEffects {

    private BombUpgradeEffects() {
    }

    public static int goldCostForTier(int tier) {
        return tier * 3;
    }

    public static String tierTitle(BombUpgradePath path, int tier) {
        return tierTitle(path, tier, GameLocale.FR);
    }

    public static String tierTitle(BombUpgradePath path, int tier, GameLocale loc) {
        if (loc == GameLocale.EN) {
            return switch (path) {
                case SALVO -> switch (tier) {
                    case 1 -> "I — Double volley";
                    case 2 -> "II — Burst";
                    case 3 -> "III — Rain";
                    case 4 -> "IV — Deluge";
                    case 5 -> "V — Storm";
                    default -> "?";
                };
                case RICOCHET -> switch (tier) {
                    case 1 -> "I — Bounce";
                    case 2 -> "II — Double jump";
                    case 3 -> "III — Ping-pong";
                    case 4 -> "IV — Rampage";
                    case 5 -> "V — Infinite (max 8)";
                    default -> "?";
                };
                case OVERLOAD -> switch (tier) {
                    case 1 -> "I — Spark";
                    case 2 -> "II — Shock";
                    case 3 -> "III — Blaze";
                    case 4 -> "IV — Cataclysm";
                    case 5 -> "V — Apocalypse";
                    default -> "?";
                };
            };
        }
        return switch (path) {
            case SALVO -> switch (tier) {
                case 1 -> "I — Double salve";
                case 2 -> "II — Rafale";
                case 3 -> "III — Pluie";
                case 4 -> "IV — Deluge";
                case 5 -> "V — Tempete";
                default -> "?";
            };
            case RICOCHET -> switch (tier) {
                case 1 -> "I — Rebond";
                case 2 -> "II — Double saut";
                case 3 -> "III — Ping-pong";
                case 4 -> "IV — Fou furieux";
                case 5 -> "V — Infini (max 8)";
                default -> "?";
            };
            case OVERLOAD -> switch (tier) {
                case 1 -> "I — Etincelle";
                case 2 -> "II — Choc";
                case 3 -> "III — Brasier";
                case 4 -> "IV — Cataclysme";
                case 5 -> "V — Apocalypse";
                default -> "?";
            };
        };
    }

    public static List<String> tierLines(BombUpgradePath path, int tier) {
        return switch (path) {
            case SALVO -> switch (tier) {
                case 1 -> List.of("+1 bombe par tir");
                case 2 -> List.of("+2 bombes");
                case 3 -> List.of("+3 bombes");
                case 4 -> List.of("+4 bombes");
                case 5 -> List.of("+5 bombes");
                default -> List.of();
            };
            case RICOCHET -> switch (tier) {
                case 1 -> List.of("1 rebond");
                case 2 -> List.of("2 rebonds");
                case 3 -> List.of("3 rebonds");
                case 4 -> List.of("5 rebonds");
                case 5 -> List.of("jusqu'a 8 rebonds");
                default -> List.of();
            };
            case OVERLOAD -> switch (tier) {
                case 1 -> List.of("+8% rayon explosion");
                case 2 -> List.of("+18% rayon");
                case 3 -> List.of("+12% degats zone");
                case 4 -> List.of("+22% degats");
                case 5 -> List.of("+35% rayon et degats");
                default -> List.of();
            };
        };
    }

    public static int extraBombsPerShot(ItemStack stack, BombUpgradeState st) {
        if (st.path() != BombUpgradePath.SALVO) {
            return 0;
        }
        return st.tier();
    }

    public static int maxBounces(ItemStack stack, BombUpgradeState st) {
        if (st.path() != BombUpgradePath.RICOCHET) {
            return 0;
        }
        return switch (st.tier()) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 5;
            case 5 -> 8;
            default -> 0;
        };
    }

    public static double radiusMul(BombUpgradeState st) {
        if (st.isEmpty() || st.path() != BombUpgradePath.OVERLOAD) {
            return 1.0;
        }
        return 1.0 + 0.07 * st.tier() * st.tier();
    }

    public static double damageMul(BombUpgradeState st) {
        if (st.isEmpty() || st.path() != BombUpgradePath.OVERLOAD) {
            return 1.0;
        }
        return 1.0 + 0.09 * st.tier() * st.tier();
    }

    public static boolean canBuyTier(BombUpgradeState current, BombUpgradePath path, int tier) {
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

    public static boolean pathLocked(BombUpgradeState current, BombUpgradePath path) {
        return !current.isEmpty() && current.path() != path;
    }

    public static boolean tierOwned(BombUpgradeState current, BombUpgradePath path, int tier) {
        return !current.isEmpty() && current.path() == path && current.tier() >= tier;
    }

    public static List<Component> loreLines(BombUpgradeState st) {
        return loreLines(st, GameLocale.FR);
    }

    public static List<Component> loreLines(BombUpgradeState st, GameLocale loc) {
        List<Component> lines = new ArrayList<>();
        if (st.isEmpty()) {
            return lines;
        }
        lines.add(Component.text(I18n.string(loc, "upgrade.bomb_header", st.path().styleName(loc)), NamedTextColor.LIGHT_PURPLE));
        for (int t = 1; t <= st.tier(); t++) {
            lines.add(Component.text("  " + tierTitle(st.path(), t, loc), NamedTextColor.GOLD));
        }
        return lines;
    }
}
