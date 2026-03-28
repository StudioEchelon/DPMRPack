package fr.dpmr.command;

import fr.dpmr.data.PointsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TopCommand implements CommandExecutor {

    private final PointsManager pointsManager;

    public TopCommand(PointsManager pointsManager) {
        this.pointsManager = pointsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean killsBoard = args.length > 0 && (args[0].equalsIgnoreCase("kill") || args[0].equalsIgnoreCase("kills"));
        if (args.length > 0 && !killsBoard && !args[0].equalsIgnoreCase("points")) {
            sender.sendMessage(Component.text("Usage: /" + label.toLowerCase(Locale.ROOT) + " [kill|points]", NamedTextColor.YELLOW));
            return true;
        }
        List<Map.Entry<UUID, Integer>> top = killsBoard ? pointsManager.getTopKills(10) : pointsManager.getTop(10);
        sender.sendMessage(Component.text(killsBoard ? "=== TOP KILLS DPMR ===" : "=== TOP DPMR ===", NamedTextColor.AQUA));
        if (top.isEmpty()) {
            sender.sendMessage(Component.text(killsBoard ? "Aucun kill pour le moment." : "Aucun point pour le moment.", NamedTextColor.GRAY));
            return true;
        }
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            String suffix = killsBoard ? " kills" : " points";
            sender.sendMessage(Component.text(rank + ". " + pointsManager.resolveName(entry.getKey()) + " - " + entry.getValue() + suffix, NamedTextColor.GOLD));
            rank++;
        }
        return true;
    }
}
