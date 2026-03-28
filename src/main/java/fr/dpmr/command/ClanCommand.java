package fr.dpmr.command;

import fr.dpmr.data.ClanManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClanCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public ClanCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande reservee aux joueurs.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /clan <create|join|leave|info|top>", NamedTextColor.YELLOW));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /clan create <nom>", NamedTextColor.YELLOW));
                    return true;
                }
                String name = args[1];
                if (clanManager.createClan(name, player.getUniqueId())) {
                    clanManager.save();
                    player.sendMessage(Component.text("Clan cree: " + name, NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Impossible de creer ce clan.", NamedTextColor.RED));
                }
            }
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /clan join <nom>", NamedTextColor.YELLOW));
                    return true;
                }
                if (clanManager.joinClan(args[1], player.getUniqueId())) {
                    clanManager.save();
                    player.sendMessage(Component.text("Tu as rejoint le clan " + args[1], NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Impossible de rejoindre ce clan.", NamedTextColor.RED));
                }
            }
            case "leave" -> {
                if (clanManager.leaveClan(player.getUniqueId())) {
                    clanManager.save();
                    player.sendMessage(Component.text("Tu as quitte ton clan.", NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("Tu n'es dans aucun clan.", NamedTextColor.RED));
                }
            }
            case "info" -> {
                String clan = clanManager.getPlayerClan(player.getUniqueId());
                if (clan == null) {
                    player.sendMessage(Component.text("Tu n'es dans aucun clan.", NamedTextColor.RED));
                    return true;
                }
                Set<UUID> members = clanManager.getClans().get(clan);
                int size = members != null ? members.size() : 0;
                player.sendMessage(Component.text("Clan: " + clan + " (" + size + " membres)", NamedTextColor.AQUA));
            }
            case "top" -> {
                player.sendMessage(Component.text("=== TOP CLANS ===", NamedTextColor.AQUA));
                clanManager.getClans().entrySet().stream()
                        .sorted(Comparator.comparingInt((Map.Entry<String, Set<UUID>> e) -> e.getValue().size()).reversed())
                        .limit(10)
                        .forEach(e -> player.sendMessage(Component.text(e.getKey() + " - " + e.getValue().size() + " membres", NamedTextColor.GOLD)));
            }
            default -> player.sendMessage(Component.text("Sous-commande inconnue.", NamedTextColor.RED));
        }
        return true;
    }
}
