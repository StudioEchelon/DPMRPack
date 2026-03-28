package fr.dpmr.command;

import fr.dpmr.game.TopHologramManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HologramTopCommand implements CommandExecutor {

    private final TopHologramManager hologramManager;

    public HologramTopCommand(TopHologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Commande reservee aux joueurs.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("dpmr.admin")) {
            player.sendMessage(Component.text("Permission refusee.", NamedTextColor.RED));
            return true;
        }
        if (args.length > 0 && ("remove".equalsIgnoreCase(args[0]) || "retirer".equalsIgnoreCase(args[0]))) {
            boolean ok = hologramManager.removeNearestColumn(player, 6.0);
            if (!ok) {
                player.sendMessage(Component.text("Aucun hologramme top 10 a portee (~6 blocs).", NamedTextColor.RED));
            }
            return true;
        }
        hologramManager.addColumn(player);
        return true;
    }
}
