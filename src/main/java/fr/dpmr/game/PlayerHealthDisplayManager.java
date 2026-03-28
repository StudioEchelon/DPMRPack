package fr.dpmr.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Affiche la vie au-dessus du nom des joueurs.
 */
public class PlayerHealthDisplayManager {

    private static final String OBJECTIVE_ID = "dpmr_health";
    private Objective objective;

    public void enable() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective existing = main.getObjective(OBJECTIVE_ID);
        if (existing != null) {
            objective = existing;
        } else {
            objective = main.registerNewObjective(
                    OBJECTIVE_ID,
                    Criteria.HEALTH,
                    Component.text("❤", NamedTextColor.RED),
                    RenderType.HEARTS
            );
        }
        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    public void disable() {
        if (objective != null) {
            objective.unregister();
            objective = null;
            return;
        }
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective existing = main.getObjective(OBJECTIVE_ID);
        if (existing != null) {
            existing.unregister();
        }
    }
}

