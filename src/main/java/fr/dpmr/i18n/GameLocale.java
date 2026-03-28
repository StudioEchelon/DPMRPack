package fr.dpmr.i18n;

import java.util.Locale;

public enum GameLocale {
    EN("English"),
    FR("Français");

    private final String label;

    GameLocale(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static GameLocale fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return EN;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (u.startsWith("FR")) {
            return FR;
        }
        return EN;
    }
}
