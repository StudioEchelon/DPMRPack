package fr.dpmr.game;

import fr.dpmr.npc.citizens.CitizensCombatNpcBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

public class SafeRespawnManager implements Listener {

    private final JavaPlugin plugin;

    public SafeRespawnManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("safe-respawn.spawn.world", location.getWorld().getName());
        cfg.set("safe-respawn.spawn.x", location.getX());
        cfg.set("safe-respawn.spawn.y", location.getY());
        cfg.set("safe-respawn.spawn.z", location.getZ());
        cfg.set("safe-respawn.spawn.yaw", location.getYaw());
        cfg.set("safe-respawn.spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    @EventHandler
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        /* PNJ combat DPMR (Citizens = Player) : laisser mourir, pas le “sauvetage” des vrais joueurs. */
        if (player.getPersistentDataContainer().has(new NamespacedKey(plugin, "dpmr_fake_npc"), PersistentDataType.BYTE)) {
            return;
        }
        /* Autres NPC Citizens (hors joueurs) : pas de safe-respawn non plus. */
        if (CitizensCombatNpcBridge.isCitizensNpcEntity(player)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("safe-respawn.enabled", true)) {
            return;
        }
        double finalDamage = event.getFinalDamage();
        if (player.getHealth() - finalDamage > 0.0) {
            return;
        }
        event.setCancelled(true);
        Location spawn = readSpawn();
        var maxAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxAttr != null ? maxAttr.getValue() : 20.0;
        player.setHealth(Math.max(1.0, Math.min(max, plugin.getConfig().getDouble("safe-respawn.respawn-health", max))));
        player.setFireTicks(0);
        if (spawn != null) {
            player.teleport(spawn);
        }
        KillContext context = resolveKillContext(event);
        if (context.killer != null && !context.killer.getUniqueId().equals(player.getUniqueId())) {
            Bukkit.broadcast(Component.text("✦ ", NamedTextColor.GOLD)
                    .append(Component.text(context.killer.getName(), NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text(" a elimine ", NamedTextColor.GRAY))
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text(" avec ", NamedTextColor.GRAY))
                    .append(Component.text(context.weaponName, NamedTextColor.AQUA))
                    .append(Component.text(" ✧", NamedTextColor.GOLD)));
        }
        player.sendActionBar(Component.text("Tu as ete sauve et teleporte au spawn.", NamedTextColor.YELLOW));
    }

    private Location readSpawn() {
        FileConfiguration cfg = plugin.getConfig();
        String worldName = cfg.getString("safe-respawn.spawn.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            return null;
        }
        double x = cfg.getDouble("safe-respawn.spawn.x", world.getSpawnLocation().getX());
        double y = cfg.getDouble("safe-respawn.spawn.y", world.getSpawnLocation().getY());
        double z = cfg.getDouble("safe-respawn.spawn.z", world.getSpawnLocation().getZ());
        float yaw = (float) cfg.getDouble("safe-respawn.spawn.yaw", world.getSpawnLocation().getYaw());
        float pitch = (float) cfg.getDouble("safe-respawn.spawn.pitch", world.getSpawnLocation().getPitch());
        return new Location(world, x, y, z, yaw, pitch);
    }

    private KillContext resolveKillContext(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return new KillContext(null, "inconnu");
        }
        Entity damager = byEntity.getDamager();
        if (damager instanceof Player p) {
            return new KillContext(p, readWeaponNameFromHand(p));
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            String weaponName = readWeaponNameFromProjectile(proj);
            if (weaponName == null) {
                weaponName = readWeaponNameFromHand(p);
            }
            return new KillContext(p, weaponName);
        }
        return new KillContext(null, "inconnu");
    }

    private String readWeaponNameFromHand(Player killer) {
        ItemStack hand = killer.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            return "poings";
        }
        if (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
            Component display = hand.getItemMeta().displayName();
            if (display != null) {
                String plain = PlainTextComponentSerializer.plainText().serialize(display).trim();
                if (!plain.isEmpty()) {
                    return plain;
                }
            }
        }
        return hand.getType().name().toLowerCase().replace('_', ' ');
    }

    private String readWeaponNameFromProjectile(Projectile projectile) {
        String weaponId = projectile.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "dpmr_proj_weapon"), PersistentDataType.STRING);
        WeaponProfile profile = WeaponProfile.fromId(weaponId);
        return profile != null ? "ARME " + profile.displayName() : null;
    }

    private static final class KillContext {
        private final Player killer;
        private final String weaponName;

        private KillContext(Player killer, String weaponName) {
            this.killer = killer;
            this.weaponName = weaponName;
        }
    }
}

