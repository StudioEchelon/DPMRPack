package fr.dpmr.command;

import fr.dpmr.cosmetics.CosmeticProfile;
import fr.dpmr.cosmetics.CosmeticsGui;
import fr.dpmr.cosmetics.CosmeticsManager;
import fr.dpmr.data.PointsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class CosmeticsCommand implements CommandExecutor {

    private final CosmeticsGui gui;
    private final CosmeticsManager cosmeticsManager;
    private final PointsManager pointsManager;

    public CosmeticsCommand(CosmeticsGui gui, CosmeticsManager cosmeticsManager, PointsManager pointsManager) {
        this.gui = gui;
        this.cosmeticsManager = cosmeticsManager;
        this.pointsManager = pointsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Commande reservee aux joueurs.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            gui.open(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("buy") && args.length >= 2) {
            CosmeticProfile p = CosmeticProfile.fromId(args[1]);
            if (p == null) {
                player.sendMessage(Component.text("Cosmetic inconnu.", NamedTextColor.RED));
                return true;
            }
            int pts = pointsManager.getPoints(player.getUniqueId());
            if (pts < p.price()) {
                player.sendMessage(Component.text("Pas assez de points (" + pts + "/" + p.price() + ").", NamedTextColor.RED));
                return true;
            }
            cosmeticsManager.buy(player.getUniqueId(), p);
            cosmeticsManager.setSelected(player.getUniqueId(), p);
            player.sendMessage(Component.text("Achete: " + p.displayName(), NamedTextColor.GREEN));
            if (p.type().name().equals("VANITY")) {
                cosmeticsManager.giveVanity(player, p);
            }
            return true;
        }
        player.sendMessage(Component.text("Usage: /cosmetics | /cosmetics buy <id>", NamedTextColor.YELLOW));
        return true;
    }
}

