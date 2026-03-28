package fr.dpmr.command;

import fr.dpmr.npc.NpcSpawnerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Retire tous les PNJ de combat DPMR (meme logique que /dpmr killnpcs), sans passer par la commande admin complete.
 */
public final class KillDpmNpcsCommand implements CommandExecutor {

    private final NpcSpawnerManager npcSpawnerManager;

    public KillDpmNpcsCommand(NpcSpawnerManager npcSpawnerManager) {
        this.npcSpawnerManager = npcSpawnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dpmr.killdpmrnpcs")) {
            sender.sendMessage(Component.text("Permission refusee.", NamedTextColor.RED));
            return true;
        }
        int removed = npcSpawnerManager.killAllSpawnedFakeNpcs();
        sender.sendMessage(Component.text("PNJ DPMR retires sur la carte : " + removed, NamedTextColor.GREEN));
        return true;
    }
}
