package fr.dpmr.command;

import fr.dpmr.i18n.GameLocale;
import fr.dpmr.i18n.I18n;
import fr.dpmr.i18n.PlayerLanguageStore;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class LangCommand implements CommandExecutor {

    private final PlayerLanguageStore languageStore;

    public LangCommand(PlayerLanguageStore languageStore) {
        this.languageStore = languageStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(I18n.component(player, NamedTextColor.AQUA, "lang.current", languageStore.get(player).label()));
            player.sendMessage(I18n.component(player, NamedTextColor.GRAY, "lang.usage"));
            return true;
        }
        String code = args[0].toLowerCase(Locale.ROOT);
        GameLocale loc;
        if (code.startsWith("fr")) {
            loc = GameLocale.FR;
        } else if (code.startsWith("en")) {
            loc = GameLocale.EN;
        } else {
            player.sendMessage(I18n.component(player, NamedTextColor.RED, "lang.unknown"));
            return true;
        }
        languageStore.set(player, loc);
        player.sendMessage(I18n.component(player, NamedTextColor.GREEN, "lang.set", loc.label()));
        return true;
    }
}
