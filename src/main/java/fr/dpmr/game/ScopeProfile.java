package fr.dpmr.game;

import org.bukkit.entity.Player;

/**
 * Lunette (sneak) : dispersion serrée, portée et dégâts améliorés.
 * Le FOV client ne peut pas être modifié sans mod ; on simule avec lenteur + précision.
 */
public record ScopeProfile(
        boolean enabled,
        /** Dispersion en visée (sneak), en degrés */
        double aimedSpreadDegrees,
        /** Multiplicateur de portée en visée */
        double rangeMulAimed,
        /** Multiplicateur de dégâts en visée */
        double damageMulAimed,
        /** Multiplicateur de dispersion hanche (pas sneak) pour snipers */
        double hipFireSpreadMul
) {
    public static final ScopeProfile NONE = new ScopeProfile(false, 0, 1, 1, 1);

    public static ScopeProfile sniper(double aimedSpreadDeg, double rangeMul, double damageMul, double hipMul) {
        return new ScopeProfile(true, aimedSpreadDeg, rangeMul, damageMul, hipMul);
    }

    public double spreadDegrees(WeaponProfile w, Player p) {
        if (!enabled) {
            return w.baseSpreadDegrees();
        }
        if (p.isSneaking()) {
            return Math.max(0.006, aimedSpreadDegrees);
        }
        return w.baseSpreadDegrees() * hipFireSpreadMul;
    }

    public double range(WeaponProfile w, Player p) {
        if (!enabled || !p.isSneaking()) {
            return w.baseRange();
        }
        return w.baseRange() * rangeMulAimed;
    }

    public double damage(WeaponProfile w, Player p) {
        if (!enabled || !p.isSneaking()) {
            return w.baseDamage();
        }
        return w.baseDamage() * damageMulAimed;
    }
}
