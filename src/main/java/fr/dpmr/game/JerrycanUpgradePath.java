package fr.dpmr.game;

import fr.dpmr.i18n.GameLocale;

import java.util.Locale;

/**
 * Voies exclusives du J-20 (jerrican) — atelier d'armement.
 */
public enum JerrycanUpgradePath {
    THERMAL("THERM", "Expansion thermique", "Rayon, brulure, portee de lancer, propagation, enfer blanc"),
    VISCOUS("VISQ", "Persistance visqueuse", "Ralentissement, duree de feu, cumuls, etreinte noire"),
    BREACH("BREACH", "Tactique de breche", "Cadence, impact, capacite, fumee, pluie de combustible");

    private final String shortLabel;
    private final String styleName;
    private final String blurb;

    JerrycanUpgradePath(String shortLabel, String styleName, String blurb) {
        this.shortLabel = shortLabel;
        this.styleName = styleName;
        this.blurb = blurb;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public String styleName() {
        return styleName;
    }

    public String blurb() {
        return blurb;
    }

    public String styleName(GameLocale loc) {
        if (loc == GameLocale.FR) {
            return styleName;
        }
        return switch (this) {
            case THERMAL -> "Thermal expansion";
            case VISCOUS -> "Viscous persistence";
            case BREACH -> "Breacher tactics";
        };
    }

    public static JerrycanUpgradePath fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
