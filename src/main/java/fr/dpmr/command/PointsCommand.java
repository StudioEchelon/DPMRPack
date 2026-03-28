package fr.dpmr.command;

import fr.dpmr.data.PointsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PointsCommand implements CommandExecutor {

    private final PointsManager pointsManager;

    public PointsCommand(PointsManager pointsManager) {
        this.pointsManager = pointsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("dpmr.admin")) {
                sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /points give <joueur> <montant>", NamedTextColor.YELLOW));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Montant invalide.", NamedTextColor.RED));
                return true;
            }
            amount = Math.max(0, amount);
            pointsManager.addPoints(target.getUniqueId(), amount);
            pointsManager.save();
            sender.sendMessage(Component.text("+" + amount + " points -> " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text("Tu as recu +" + amount + " points.", NamedTextColor.GOLD));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /points [joueur] | /points give <joueur> <montant>");
                return true;
            }
            int points = pointsManager.getPoints(player.getUniqueId());
            sender.sendMessage(Component.text("Tu as " + points + " points.", NamedTextColor.GOLD));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
            return true;
        }
        int points = pointsManager.getPoints(target.getUniqueId());
        sender.sendMessage(Component.text(target.getName() + " a " + points + " points.", NamedTextColor.YELLOW));
        return true;
    }
}
