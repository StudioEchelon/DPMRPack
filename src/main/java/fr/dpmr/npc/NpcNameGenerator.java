package fr.dpmr.npc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Prenoms + tags pseudo charges depuis {@code /npc/npc-first-names.txt} et {@code /npc/npc-pseudo-suffixes.txt}.
 */
public final class NpcNameGenerator {

    private static final int MAX_NAME_LENGTH = 24;
    private static final String[] FIRST = loadLines("/npc/npc-first-names.txt", defaultFirst());
    private static final String[] TAGS = loadLines("/npc/npc-pseudo-suffixes.txt", defaultTags());

    private NpcNameGenerator() {
    }

    public static String randomDisplayName() {
        return randomDisplayName(ThreadLocalRandom.current());
    }

    public static String randomDisplayName(ThreadLocalRandom r) {
        if (FIRST.length == 0) {
            return "Soldat";
        }
        String first = FIRST[r.nextInt(FIRST.length)];
        if (TAGS.length == 0) {
            return clamp(first + r.nextInt(100));
        }
        String tag = TAGS[r.nextInt(TAGS.length)];
        int mode = r.nextInt(100);
        String raw = switch (mode) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 -> first;
            case 20, 21, 22, 23, 24, 25, 26, 27, 28, 29 -> first + "_" + tag;
            case 30, 31, 32, 33, 34, 35, 36, 37, 38, 39 -> tag + "_" + first;
            case 40, 41, 42, 43, 44, 45, 46, 47, 48, 49 -> "x" + tag + first;
            case 50, 51, 52, 53, 54, 55, 56, 57, 58, 59 -> first + r.nextInt(100);
            case 60, 61, 62, 63, 64, 65, 66, 67, 68, 69 -> tag + r.nextInt(1000);
            case 70, 71, 72, 73, 74 -> first + "_" + r.nextInt(100);
            case 75, 76, 77, 78, 79 -> "_" + tag + "_" + first;
            default -> first + "_" + tag + r.nextInt(100);
        };
        return clamp(raw);
    }

    private static String clamp(String s) {
        if (s == null || s.isBlank()) {
            return "PNJ";
        }
        String t = s.trim();
        return t.length() <= MAX_NAME_LENGTH ? t : t.substring(0, MAX_NAME_LENGTH);
    }

    private static String[] loadLines(String resourcePath, String[] fallback) {
        List<String> out = new ArrayList<>(256);
        try (InputStream in = NpcNameGenerator.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            out.add(line);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return out.isEmpty() ? fallback : out.toArray(new String[0]);
    }

    private static String[] defaultFirst() {
        return new String[]{
                "Lucas", "Thomas", "Marc", "Hugo", "Paul", "Nathan", "Leo", "Adam", "Alex", "Jordan"
        };
    }

    private static String[] defaultTags() {
        return new String[]{
                "Snype", "NoScope", "Rush", "Clutch", "Shadow", "Ghost", "Viper", "Storm", "xPro", "Frag"
        };
    }
}
