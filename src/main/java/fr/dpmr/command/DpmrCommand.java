package fr.dpmr.command;

import fr.dpmr.game.ApocalypseManager;
import fr.dpmr.game.BandageManager;
import fr.dpmr.game.DpmrConsumable;
import fr.dpmr.game.DynamicObjectiveManager;
import fr.dpmr.game.LaunchpadManager;
import fr.dpmr.game.LaunchpadStyle;
import fr.dpmr.game.LootManager;
import fr.dpmr.game.ModificationTableRegistry;
import fr.dpmr.game.FamiliarPetManager;
import fr.dpmr.game.SafeRespawnManager;
import fr.dpmr.game.PetType;
import fr.dpmr.game.WeaponManager;
import fr.dpmr.crate.CrateManager;
import fr.dpmr.npc.NpcSpawnerManager;
import fr.dpmr.npc.WarWorldNpcManager;
import fr.dpmr.resourcepack.ResourcePackManager;
import fr.dpmr.armor.ArmorManager;
import fr.dpmr.armor.ArmorProfile;
import fr.dpmr.gui.GiveGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public class DpmrCommand implements CommandExecutor, TabCompleter {

    private final ApocalypseManager apocalypseManager;
    private final LootManager lootManager;
    private final WeaponManager weaponManager;
    private final BandageManager bandageManager;
    private final GiveGui giveGui;
    private final ModificationTableRegistry modTableRegistry;
    private final LaunchpadManager launchpadManager;
    private final fr.dpmr.zone.ZoneManager zoneManager;
    private final FamiliarPetManager familiarPetManager;
    private final CrateManager crateManager;
    private final NpcSpawnerManager npcSpawnerManager;
    private final ArmorManager armorManager;
    private final SafeRespawnManager safeRespawnManager;
    private final WarWorldNpcManager warWorldNpcManager;
    private final ResourcePackManager resourcePackManager;

    public DpmrCommand(ApocalypseManager apocalypseManager, LootManager lootManager, WeaponManager weaponManager,
                       BandageManager bandageManager, GiveGui giveGui, ModificationTableRegistry modTableRegistry,
                       LaunchpadManager launchpadManager, fr.dpmr.zone.ZoneManager zoneManager, FamiliarPetManager familiarPetManager,
                       CrateManager crateManager, NpcSpawnerManager npcSpawnerManager, ArmorManager armorManager,
                       SafeRespawnManager safeRespawnManager, WarWorldNpcManager warWorldNpcManager,
                       ResourcePackManager resourcePackManager) {
        this.apocalypseManager = apocalypseManager;
        this.lootManager = lootManager;
        this.weaponManager = weaponManager;
        this.bandageManager = bandageManager;
        this.giveGui = giveGui;
        this.modTableRegistry = modTableRegistry;
        this.launchpadManager = launchpadManager;
        this.zoneManager = zoneManager;
        this.familiarPetManager = familiarPetManager;
        this.crateManager = crateManager;
        this.npcSpawnerManager = npcSpawnerManager;
        this.armorManager = armorManager;
        this.safeRespawnManager = safeRespawnManager;
        this.warWorldNpcManager = warWorldNpcManager;
        this.resourcePackManager = resourcePackManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dpmr.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /dpmr <start|stop|status|...|killnpcs|giveconsumable|givelaunchpad|launchpads>", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> {
                apocalypseManager.startGame();
                sender.sendMessage(Component.text("Mode apocalypse active.", NamedTextColor.GREEN));
            }
            case "stop" -> {
                apocalypseManager.stopGame();
                sender.sendMessage(Component.text("Mode apocalypse desactive.", NamedTextColor.GRAY));
            }
            case "status" -> {
                boolean running = apocalypseManager.isGameRunning();
                sender.sendMessage(Component.text(
                        "Etat actuel: " + (running ? "EN COURS" : "ARRETE"),
                        running ? NamedTextColor.RED : NamedTextColor.GRAY
                ));
                DynamicObjectiveManager dom = apocalypseManager.getDynamicObjectiveManager();
                if (dom != null) {
                    sender.sendMessage(Component.text(dom.getStatusSummary(), NamedTextColor.DARK_AQUA));
                }
            }
            case "forceevent" -> {
                apocalypseManager.forceDisaster();
                sender.sendMessage(Component.text("Evenement catastrophe force.", NamedTextColor.LIGHT_PURPLE));
            }
            case "airdrop" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                        return true;
                    }
                    lootManager.setAirdropLocation(player);
                    sender.sendMessage(Component.text("Position airdrop enregistree.", NamedTextColor.GREEN));
                    return true;
                }
                lootManager.spawnAirdrop();
                sender.sendMessage(Component.text("Airdrop force.", NamedTextColor.AQUA));
            }
            case "forcechest" -> {
                lootManager.forceSpawnZoneChest();
                sender.sendMessage(Component.text("Tentative de spawn coffre zone.", NamedTextColor.GREEN));
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                safeRespawnManager.setSpawn(player.getLocation());
                sender.sendMessage(Component.text("Spawn de respawn DPMR defini ici.", NamedTextColor.GREEN));
            }
            case "givebandage" -> {
                int amount = 1;
                String targetName = null;
                if (args.length >= 2) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[1]));
                        if (args.length >= 3) {
                            targetName = args[2];
                        }
                    } catch (NumberFormatException ignored) {
                        targetName = args[1];
                    }
                }
                Player target;
                if (targetName != null) {
                    target = Bukkit.getPlayerExact(targetName);
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage(Component.text("Usage: /dpmr givebandage [nombre] <joueur>", NamedTextColor.YELLOW));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
                    return true;
                }
                target.getInventory().addItem(bandageManager.createBandage(amount));
                sender.sendMessage(Component.text("Bandages donnes a " + target.getName() + " x" + amount, NamedTextColor.GREEN));
            }
            case "giveconsumable" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr giveconsumable <type> [nombre] [joueur]", NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("Ex: bandage-small, bandage-medium, medikit, shield-potion-large", NamedTextColor.GRAY));
                    return true;
                }
                DpmrConsumable ct = DpmrConsumable.fromConfigKey(args[1]);
                if (ct == null) {
                    try {
                        ct = DpmrConsumable.valueOf(args[1].toUpperCase(Locale.ROOT).replace('-', '_'));
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(Component.text("Type inconnu: " + args[1], NamedTextColor.RED));
                        return true;
                    }
                }
                int amount = 1;
                String targetName = null;
                if (args.length >= 3) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[2]));
                        if (args.length >= 4) {
                            targetName = args[3];
                        }
                    } catch (NumberFormatException ignored) {
                        targetName = args[2];
                    }
                }
                Player target;
                if (targetName != null) {
                    target = Bukkit.getPlayerExact(targetName);
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage(Component.text("Usage: /dpmr giveconsumable <type> [nombre] <joueur>", NamedTextColor.YELLOW));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
                    return true;
                }
                target.getInventory().addItem(bandageManager.createConsumable(ct, amount));
                sender.sendMessage(Component.text("Objet " + ct.configKey() + " donne a " + target.getName() + " x" + amount, NamedTextColor.GREEN));
            }
            case "givechest" -> {
                int amount = 1;
                String targetName = null;
                if (args.length >= 2) {
                    try {
                        amount = Math.max(1, Math.min(64, Integer.parseInt(args[1])));
                        if (args.length >= 3) {
                            targetName = args[2];
                        }
                    } catch (NumberFormatException ignored) {
                        targetName = args[1];
                    }
                }
                Player target;
                if (targetName != null) {
                    target = Bukkit.getPlayerExact(targetName);
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage(Component.text("Usage: /dpmr givechest [nombre] <joueur>", NamedTextColor.YELLOW));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
                    return true;
                }
                target.getInventory().addItem(lootManager.createPortableLootChestItem(amount));
                sender.sendMessage(Component.text("Coffre loot DPMR donne a " + target.getName() + " x" + amount, NamedTextColor.GREEN));
            }
            case "giveaxechest" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage(Component.text("Usage: /dpmr giveaxechest [joueur]", NamedTextColor.YELLOW));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
                    return true;
                }
                target.getInventory().addItem(lootManager.createChestBreakerAxeItem());
                sender.sendMessage(Component.text("AxeChest donnee a " + target.getName(), NamedTextColor.GREEN));
            }
            case "clearholograms" -> {
                int removed = lootManager.purgeAllLootHolograms();
                sender.sendMessage(Component.text("Hologrammes supprimes: " + removed, NamedTextColor.GREEN));
            }
            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                lootManager.openAdminMenu(player);
            }
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                giveGui.openMain(player);
            }
            case "guns" -> sender.sendMessage(Component.text("Armes a feu: " + String.join(", ", weaponManager.getWarfareWeaponIds()), NamedTextColor.AQUA));
            case "launchpads" -> sender.sendMessage(Component.text(
                    "Styles launchpad: " + String.join(", ", launchpadManager.getAllStyleIds()),
                    NamedTextColor.GREEN));
            case "endportal" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr endportal <add|set|remove <index>|list|clear|off>", NamedTextColor.YELLOW));
                    return true;
                }
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("add")) {
                    int count = launchpadManager.addEndPortalTarget(player);
                    if (count < 0) {
                        sender.sendMessage(Component.text("Limite atteinte: 10 zones max.", NamedTextColor.RED));
                        return true;
                    }
                    sender.sendMessage(Component.text("Zone EndPortal ajoutee (" + count + "/10).", NamedTextColor.GREEN));
                    return true;
                }
                if (action.equals("set")) {
                    launchpadManager.setEndPortalTarget(player);
                    sender.sendMessage(Component.text("Zone unique EndPortal definie (1/10).", NamedTextColor.GREEN));
                    return true;
                }
                if (action.equals("list")) {
                    List<org.bukkit.Location> list = launchpadManager.listEndPortalTargets();
                    sender.sendMessage(Component.text("Zones EndPortal: " + list.size() + "/10", NamedTextColor.AQUA));
                    for (int i = 0; i < list.size(); i++) {
                        org.bukkit.Location l = list.get(i);
                        String world = l.getWorld() != null ? l.getWorld().getName() : "world";
                        sender.sendMessage(Component.text(
                                (i + 1) + ". " + world + " " + (int) l.getX() + " " + (int) l.getY() + " " + (int) l.getZ(),
                                NamedTextColor.GRAY));
                    }
                    return true;
                }
                if (action.equals("remove")) {
                    if (args.length < 3) {
                        sender.sendMessage(Component.text("Usage: /dpmr endportal remove <index>", NamedTextColor.YELLOW));
                        return true;
                    }
                    int idx;
                    try {
                        idx = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Component.text("Index invalide.", NamedTextColor.RED));
                        return true;
                    }
                    if (!launchpadManager.removeEndPortalTarget(idx)) {
                        sender.sendMessage(Component.text("Index introuvable.", NamedTextColor.RED));
                        return true;
                    }
                    sender.sendMessage(Component.text("Zone EndPortal retiree.", NamedTextColor.YELLOW));
                    return true;
                }
                if (action.equals("clear")) {
                    launchpadManager.clearEndPortalTargets();
                    sender.sendMessage(Component.text("Toutes les zones EndPortal ont ete videes.", NamedTextColor.YELLOW));
                    return true;
                }
                if (action.equals("off")) {
                    launchpadManager.disableEndPortalTarget();
                    sender.sendMessage(Component.text("EndPortal desactive.", NamedTextColor.GRAY));
                    return true;
                }
                sender.sendMessage(Component.text("Actions: add | set | remove | list | clear | off", NamedTextColor.RED));
            }
            case "givelaunchpad" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr givelaunchpad <style> [nombre] [joueur]", NamedTextColor.YELLOW));
                    return true;
                }
                LaunchpadStyle lp = LaunchpadStyle.fromId(args[1]);
                if (lp == null) {
                    sender.sendMessage(Component.text("Style inconnu. /dpmr launchpads", NamedTextColor.RED));
                    return true;
                }
                int amount = 8;
                String targetName = null;
                if (args.length >= 3) {
                    try {
                        amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
                        if (args.length >= 4) {
                            targetName = args[3];
                        }
                    } catch (NumberFormatException ignored) {
                        targetName = args[2];
                    }
                }
                Player target;
                if (targetName != null) {
                    target = Bukkit.getPlayerExact(targetName);
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage(Component.text("Precise un joueur depuis la console.", NamedTextColor.RED));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
                    return true;
                }
                target.getInventory().addItem(launchpadManager.createItem(lp, amount));
                sender.sendMessage(Component.text("Launchpads " + lp.id() + " x" + amount + " -> " + target.getName(), NamedTextColor.GREEN));
            }
            case "modtable" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr modtable <set|unset|list> (regarde un bloc)", NamedTextColor.YELLOW));
                    return true;
                }
                String m = args[1].toLowerCase(Locale.ROOT);
                if (m.equals("list")) {
                    sender.sendMessage(Component.text("Tables atelier: " + modTableRegistry.count(), NamedTextColor.AQUA));
                    for (String k : modTableRegistry.listKeys()) {
                        sender.sendMessage(Component.text(" - " + k, NamedTextColor.GRAY));
                    }
                    return true;
                }
                Block target = player.getTargetBlockExact(6);
                if (target == null || target.getType().isAir()) {
                    sender.sendMessage(Component.text("Regarde un bloc solide (6 blocs).", NamedTextColor.RED));
                    return true;
                }
                if (m.equals("set")) {
                    if (modTableRegistry.add(target)) {
                        sender.sendMessage(Component.text("Table d'armement enregistree ici.", NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text("Deja enregistree.", NamedTextColor.GRAY));
                    }
                } else if (m.equals("unset")) {
                    if (modTableRegistry.remove(target)) {
                        sender.sendMessage(Component.text("Table retiree.", NamedTextColor.YELLOW));
                    } else {
                        sender.sendMessage(Component.text("Pas une table enregistree.", NamedTextColor.GRAY));
                    }
                } else {
                    sender.sendMessage(Component.text("set | unset | list", NamedTextColor.RED));
                }
            }
            case "givegun" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr givegun <id> [joueur]", NamedTextColor.YELLOW));
                    return true;
                }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage(Component.text("Precise un joueur depuis la console.", NamedTextColor.RED));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
                    return true;
                }
                var wp = fr.dpmr.game.WeaponProfile.fromId(args[1]);
                var gun = weaponManager.createWeaponItem(args[1], target);
                if (gun == null || wp == null) {
                    sender.sendMessage(Component.text(
                            "ID d'arme inconnu. /dpmr guns (combat) ou tabulation pour la liste complete.",
                            NamedTextColor.RED));
                    return true;
                }
                target.getInventory().addItem(gun);
                sender.sendMessage(Component.text("Arme donnee a " + target.getName(), NamedTextColor.GREEN));
            }
            case "givearmor" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr givearmor <ASSAULT|HEAVY|BREACHER|MARKSMAN|EOD> [joueur]", NamedTextColor.YELLOW));
                    return true;
                }
                ArmorProfile profile = ArmorProfile.fromId(args[1]);
                if (profile == null) {
                    sender.sendMessage(Component.text("Profil armure invalide.", NamedTextColor.RED));
                    return true;
                }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage(Component.text("Precise un joueur depuis la console.", NamedTextColor.RED));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED));
                    return true;
                }
                target.getInventory().addItem(armorManager.createArmorSet(profile).toArray(ItemStack[]::new));
                sender.sendMessage(Component.text("Set armure " + profile.name() + " donne a " + target.getName(), NamedTextColor.GREEN));
            }
            case "zone" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr zone <wand|safe|war|info|clearpoints|pointinfo> ...", NamedTextColor.YELLOW));
                    return true;
                }
                String t = args[1].toLowerCase(Locale.ROOT);
                if (t.equals("info")) {
                    zoneManager.describeTo(player);
                    return true;
                }
                if (t.equals("clearpoints")) {
                    zoneManager.clearPolyPoints(player);
                    return true;
                }
                if (t.equals("pointinfo")) {
                    sender.sendMessage(Component.text("Points selectionnes: " + zoneManager.getPolyPointCount(player), NamedTextColor.AQUA));
                    return true;
                }
                if (t.equals("wand")) {
                    player.getInventory().addItem(zoneManager.createZoneWand("sel", 0));
                    sender.sendMessage(Component.text("Wand de selection donnee (style WorldEdit).", NamedTextColor.GREEN));
                    return true;
                }
                if (!t.equals("safe") && !t.equals("war")) {
                    sender.sendMessage(Component.text("wand | safe | war | info | clearpoints | pointinfo", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /dpmr zone " + t + " <set <rayon>|setsel|setpoly|delete>", NamedTextColor.YELLOW));
                    return true;
                }
                String action = args[2].toLowerCase(Locale.ROOT);
                if (action.equals("delete")) {
                    if (t.equals("safe")) {
                        zoneManager.deleteSafeZone(player);
                    } else {
                        zoneManager.deleteWarZone(player);
                    }
                    return true;
                }
                if (action.equals("setsel")) {
                    if (t.equals("safe")) {
                        zoneManager.setSafeZoneFromSelection(player);
                    } else {
                        zoneManager.setWarZoneFromSelection(player);
                    }
                    return true;
                }
                if (action.equals("setpoly")) {
                    if (t.equals("safe")) {
                        zoneManager.setSafeZoneFromPoly(player);
                    } else {
                        zoneManager.setWarZoneFromPoly(player);
                    }
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /dpmr zone " + t + " " + action + " <rayon>", NamedTextColor.YELLOW));
                    return true;
                }
                double r;
                try {
                    r = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Rayon invalide.", NamedTextColor.RED));
                    return true;
                }
                if (action.equals("set")) {
                    if (t.equals("safe")) {
                        zoneManager.setSafeZone(player, r);
                    } else {
                        zoneManager.setWarZone(player, r);
                    }
                    return true;
                }
                sender.sendMessage(Component.text("Actions: set | setsel | setpoly | delete", NamedTextColor.RED));
            }
            case "pet" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr pet <gunner|medic|sniper|scout|brute|dismiss>", NamedTextColor.YELLOW));
                    return true;
                }
                String a = args[1].toLowerCase(Locale.ROOT);
                if (a.equals("dismiss")) {
                    familiarPetManager.dismiss(player.getUniqueId());
                    player.sendMessage(Component.text("Familier retire.", NamedTextColor.GRAY));
                    return true;
                }
                PetType type = PetType.fromArg(a);
                if (type != null) {
                    familiarPetManager.summon(player, type);
                    return true;
                }
                sender.sendMessage(Component.text("Types: gunner medic sniper scout brute — dismiss pour retirer", NamedTextColor.RED));
            }
            case "crate" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr crate <create|delete|list|key> ...", NamedTextColor.YELLOW));
                    return true;
                }
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("list")) {
                    sender.sendMessage(Component.text("Caisses: " + String.join(", ", crateManager.listCrates()), NamedTextColor.AQUA));
                    return true;
                }
                if (action.equals("create")) {
                    if (args.length < 3) {
                        sender.sendMessage(Component.text("Usage: /dpmr crate create <id>", NamedTextColor.YELLOW));
                        return true;
                    }
                    Block b = player.getTargetBlockExact(6);
                    if (b == null || b.getType().isAir()) {
                        sender.sendMessage(Component.text("Regarde un bloc valide (6 blocs).", NamedTextColor.RED));
                        return true;
                    }
                    crateManager.createCrate(player, args[2], b.getLocation());
                    return true;
                }
                if (action.equals("delete")) {
                    if (args.length < 3) {
                        sender.sendMessage(Component.text("Usage: /dpmr crate delete <id>", NamedTextColor.YELLOW));
                        return true;
                    }
                    crateManager.deleteCrate(player, args[2]);
                    return true;
                }
                if (action.equals("key")) {
                    if (args.length < 3) {
                        sender.sendMessage(Component.text("Usage: /dpmr crate key <id> [nombre]", NamedTextColor.YELLOW));
                        return true;
                    }
                    int amount = 1;
                    if (args.length >= 4) {
                        try {
                            amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Nombre invalide.", NamedTextColor.RED));
                            return true;
                        }
                    }
                    player.getInventory().addItem(crateManager.createKey(args[2], amount));
                    sender.sendMessage(Component.text("Cles donnees: " + args[2] + " x" + amount, NamedTextColor.GREEN));
                    return true;
                }
                sender.sendMessage(Component.text("Actions crate: create | delete | list | key", NamedTextColor.RED));
            }
            case "npcspawner" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /dpmr npcspawner <create|delete|list>", NamedTextColor.YELLOW));
                    return true;
                }
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("list")) {
                    sender.sendMessage(Component.text("NPC spawners: " + String.join(", ", npcSpawnerManager.list()), NamedTextColor.AQUA));
                    return true;
                }
                if (action.equals("create")) {
                    if (args.length < 3) {
                        sender.sendMessage(Component.text("Usage: /dpmr npcspawner create <id> [military|zombie|raider] [points] [or_min] [or_max]", NamedTextColor.YELLOW));
                        sender.sendMessage(Component.text("Regarde un bloc spawner (mob spawner).", NamedTextColor.GRAY));
                        return true;
                    }
                    Block b = player.getTargetBlockExact(6);
                    if (b == null || b.getType().isAir() || b.getType() != Material.SPAWNER) {
                        sender.sendMessage(Component.text("Regarde un bloc spawner (mob spawner, 6 blocs).", NamedTextColor.RED));
                        return true;
                    }
                    String kind = args.length >= 4 ? args[3] : "military";
                    int reward = 4;
                    if (args.length >= 5) {
                        try {
                            reward = Math.max(1, Math.min(20, Integer.parseInt(args[4])));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Points invalides.", NamedTextColor.RED));
                            return true;
                        }
                    }
                    int goldMin = 1;
                    int goldMax = 3;
                    if (args.length >= 6) {
                        try {
                            goldMin = Math.max(0, Integer.parseInt(args[5]));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("or_min invalide.", NamedTextColor.RED));
                            return true;
                        }
                    }
                    if (args.length >= 7) {
                        try {
                            goldMax = Math.max(goldMin, Integer.parseInt(args[6]));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("or_max invalide.", NamedTextColor.RED));
                            return true;
                        }
                    }
                    npcSpawnerManager.create(player, args[2], b.getLocation(), kind, reward, goldMin, goldMax);
                    return true;
                }
                if (action.equals("delete")) {
                    if (args.length < 3) {
                        sender.sendMessage(Component.text("Usage: /dpmr npcspawner delete <id>", NamedTextColor.YELLOW));
                        return true;
                    }
                    npcSpawnerManager.delete(player, args[2]);
                    return true;
                }
                sender.sendMessage(Component.text("Actions npcspawner: create | delete | list", NamedTextColor.RED));
            }
            case "killnpcs" -> {
                int removed = npcSpawnerManager.killAllSpawnedFakeNpcs();
                sender.sendMessage(Component.text("PNJ DPMR retires: " + removed, NamedTextColor.GREEN));
            }
            case "helicopter" -> {
                if (warWorldNpcManager.forceHelicopterEvent()) {
                    sender.sendMessage(Component.text("Evenement helicoptere de guerre declenche.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Impossible : zone war inactive ou aucun sol valide (voir config npc-war-world).", NamedTextColor.RED));
                }
            }
            case "warworld" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("reload")) {
                    sender.sendMessage(Component.text("Usage: /dpmr warworld reload", NamedTextColor.YELLOW));
                    return true;
                }
                npcSpawnerManager.reloadConfigAndNpcTimers();
                warWorldNpcManager.reloadSchedules();
                resourcePackManager.restartLocalPackServerAfterConfigReload();
                sender.sendMessage(Component.text("config.yml recharge + timers PNJ + resource pack (local/manifest).", NamedTextColor.GREEN));
            }
            case "resourcepack" -> resourcePackManager.sendAdminDiagnostics(sender);
            default -> sender.sendMessage(Component.text("Sous-commande inconnue.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "status", "forceevent", "menu", "gui", "airdrop", "forcechest", "setspawn", "givebandage", "giveconsumable", "givechest", "giveaxechest",
                            "clearholograms",
                            "endportal",
                            "givegun", "guns", "givearmor", "givelaunchpad", "launchpads", "modtable", "zone", "pet", "crate", "npcspawner", "killnpcs", "helicopter", "warworld", "resourcepack").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("endportal")) {
            return List.of("add", "set", "remove", "list", "clear", "off").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("endportal") && args[1].equalsIgnoreCase("remove")) {
            int n = launchpadManager.listEndPortalTargets().size();
            java.util.List<String> out = new java.util.ArrayList<>();
            for (int i = 1; i <= n; i++) {
                out.add(String.valueOf(i));
            }
            return out.stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("warworld")) {
            return List.of("reload").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("npcspawner")) {
            return List.of("create", "delete", "list").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("npcspawner") && args[1].equalsIgnoreCase("delete")) {
            return npcSpawnerManager.list().stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("npcspawner") && args[1].equalsIgnoreCase("create")) {
            return java.util.List.of("military", "zombie", "raider").stream()
                    .filter(s -> s.startsWith(args[3].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("crate")) {
            return List.of("create", "delete", "list", "key").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("crate") && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("key"))) {
            return crateManager.listCrates().stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("airdrop")) {
            return List.of("set").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("zone")) {
            return List.of("wand", "safe", "war", "info", "clearpoints", "pointinfo").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("zone") && !args[1].equalsIgnoreCase("info")) {
            return List.of("set", "setsel", "setpoly", "delete").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pet")) {
            return List.of("gunner", "medic", "sniper", "scout", "brute", "dismiss").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("giveconsumable")) {
            String p = args[1].toLowerCase(Locale.ROOT);
            return java.util.Arrays.stream(DpmrConsumable.values())
                    .map(DpmrConsumable::configKey)
                    .filter(s -> s.startsWith(p))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givegun")) {
            return weaponManager.getAllWeaponIds().stream()
                    .map(String::toLowerCase)
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givearmor")) {
            return java.util.Arrays.stream(ArmorProfile.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("givelaunchpad")) {
            return launchpadManager.getAllStyleIds().stream()
                    .map(String::toLowerCase)
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("modtable")) {
            return List.of("set", "unset", "list").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
