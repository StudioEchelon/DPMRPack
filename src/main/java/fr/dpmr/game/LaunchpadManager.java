package fr.dpmr.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.dpmr.cosmetics.CosmeticsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Launchpads : item {@link Material#SLIME_BLOCK} tague, pose = bloc propulsif.
 */
public class LaunchpadManager implements Listener {

    private static final long COOLDOWN_MS = 650L;

    private final JavaPlugin plugin;
    private final CosmeticsManager cosmeticsManager;
    private final NamespacedKey keyStyle;
    private final File file;
    private final YamlConfiguration yaml;

    private final Map<String, LaunchpadStyle> placed = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastLaunch = new ConcurrentHashMap<>();
    private final Map<UUID, Long> portalCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> parachuteFxTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> parachuteFallImmuneUntil = new ConcurrentHashMap<>();

    public LaunchpadManager(JavaPlugin plugin, CosmeticsManager cosmeticsManager) {
        this.plugin = plugin;
        this.cosmeticsManager = cosmeticsManager;
        this.keyStyle = new NamespacedKey(plugin, "dpmr_launchpad_style");
        this.file = new File(plugin.getDataFolder(), "launchpads.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public List<String> getAllStyleIds() {
        return Arrays.stream(LaunchpadStyle.values()).map(LaunchpadStyle::id).toList();
    }

    public void setEndPortalTarget(Player player) {
        clearEndPortalTargets();
        addEndPortalTarget(player);
    }

    public int addEndPortalTarget(Player player) {
        Location loc = player.getLocation();
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList("launchpads.endportal.targets"));
        if (list.size() >= 10) {
            return -1;
        }
        list.add(encodePortalTarget(loc));
        plugin.getConfig().set("launchpads.endportal.enabled", true);
        plugin.getConfig().set("launchpads.endportal.targets", list);
        plugin.getConfig().set("launchpads.endportal.target.world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        plugin.getConfig().set("launchpads.endportal.target.x", loc.getX());
        plugin.getConfig().set("launchpads.endportal.target.y", loc.getY());
        plugin.getConfig().set("launchpads.endportal.target.z", loc.getZ());
        plugin.getConfig().set("launchpads.endportal.target.yaw", loc.getYaw());
        plugin.getConfig().set("launchpads.endportal.target.pitch", loc.getPitch());
        plugin.saveConfig();
        return list.size();
    }

    public boolean removeEndPortalTarget(int index1Based) {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList("launchpads.endportal.targets"));
        int idx = index1Based - 1;
        if (idx < 0 || idx >= list.size()) {
            return false;
        }
        list.remove(idx);
        plugin.getConfig().set("launchpads.endportal.targets", list);
        plugin.saveConfig();
        return true;
    }

    public int clearEndPortalTargets() {
        plugin.getConfig().set("launchpads.endportal.targets", new ArrayList<String>());
        plugin.saveConfig();
        return 0;
    }

    public List<Location> listEndPortalTargets() {
        List<Location> out = new ArrayList<>();
        for (String raw : plugin.getConfig().getStringList("launchpads.endportal.targets")) {
            Location loc = decodePortalTarget(raw);
            if (loc != null && loc.getWorld() != null) {
                out.add(loc);
            }
        }
        return out;
    }

    public void disableEndPortalTarget() {
        plugin.getConfig().set("launchpads.endportal.enabled", false);
        plugin.saveConfig();
    }

    public ItemStack createItem(LaunchpadStyle style, int amount) {
        ItemStack item = new ItemStack(Material.SLIME_BLOCK, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Launchpad — " + style.displayFr(), style.color()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Pose le bloc : propulsion au contact", NamedTextColor.GRAY));
        lore.add(Component.text("Style: " + style.id(), NamedTextColor.DARK_GRAY));
        if (style.isParachuteStyle()) {
            lore.add(Component.text("Propulsion extreme en avant + chute lente + effet parachute", NamedTextColor.AQUA));
        }
        lore.add(Component.text("Sneak sur le bloc : pas de saut", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(keyStyle, PersistentDataType.STRING, style.name());
        item.setItemMeta(meta);
        return item;
    }

    public LaunchpadStyle readStyleFromItem(ItemStack stack) {
        if (stack == null || stack.getType() != Material.SLIME_BLOCK || !stack.hasItemMeta()) {
            return null;
        }
        String raw = stack.getItemMeta().getPersistentDataContainer().get(keyStyle, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        return LaunchpadStyle.fromId(raw);
    }

    private static String locKey(Location loc) {
        return loc.getWorld().getUID() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    public void save() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, LaunchpadStyle> e : placed.entrySet()) {
            String[] split = e.getKey().split("\\|");
            if (split.length != 4) {
                continue;
            }
            try {
                World w = Bukkit.getWorld(UUID.fromString(split[0]));
                if (w == null) {
                    continue;
                }
                int x = Integer.parseInt(split[1]);
                int y = Integer.parseInt(split[2]);
                int z = Integer.parseInt(split[3]);
                out.add(w.getName() + ";" + x + ";" + y + ";" + z + ";" + e.getValue().name());
            } catch (Exception ignored) {
            }
        }
        yaml.set("pads", out);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder launchpads.yml: " + e.getMessage());
        }
    }

    private void load() {
        placed.clear();
        for (String raw : yaml.getStringList("pads")) {
            String[] s = raw.split(";");
            if (s.length != 5) {
                continue;
            }
            World world = Bukkit.getWorld(s[0]);
            if (world == null) {
                continue;
            }
            LaunchpadStyle style = LaunchpadStyle.fromId(s[4]);
            if (style == null) {
                continue;
            }
            try {
                int x = Integer.parseInt(s[1]);
                int y = Integer.parseInt(s[2]);
                int z = Integer.parseInt(s[3]);
                Location loc = new Location(world, x, y, z);
                if (loc.getBlock().getType() == Material.SLIME_BLOCK) {
                    placed.put(locKey(loc), style);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SLIME_BLOCK) {
            return;
        }
        ItemStack hand = event.getHand() == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItemInMainHand();
        LaunchpadStyle st = readStyleFromItem(hand);
        if (st == null) {
            return;
        }
        Location loc = event.getBlockPlaced().getLocation();
        placed.put(locKey(loc), st);
        save();
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_SLIME_BLOCK_PLACE, 0.6f, 1.2f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (b.getType() != Material.SLIME_BLOCK) {
            return;
        }
        placed.remove(locKey(b.getLocation()));
        save();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        portalCooldown.remove(event.getPlayer().getUniqueId());
        BukkitTask t = parachuteFxTasks.remove(event.getPlayer().getUniqueId());
        if (t != null) {
            t.cancel();
        }
        parachuteFallImmuneUntil.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        long until = parachuteFallImmuneUntil.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() <= until) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            return;
        }
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        if (tryCustomPortalBlock(player, to)) {
            return;
        }
        Block under = to.getBlock().getRelative(BlockFace.DOWN);
        if (under.getType() != Material.SLIME_BLOCK) {
            tryEndPortalTeleport(player, to);
            return;
        }
        LaunchpadStyle style = placed.get(locKey(under.getLocation()));
        if (style == null) {
            tryEndPortalTeleport(player, to);
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLaunch.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) {
            return;
        }
        lastLaunch.put(player.getUniqueId(), now);
        launch(player, style);
    }

    private void tryEndPortalTeleport(Player player, Location to) {
        if (to.getBlock().getType() != Material.END_PORTAL) {
            return;
        }
        if (!plugin.getConfig().getBoolean("launchpads.endportal.enabled", false)) {
            return;
        }
        teleportToConfiguredPortal(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("launchpads.endportal.enabled", false)) {
            return;
        }
        if (!teleportToConfiguredPortal(player)) {
            return;
        }
        event.setCancelled(true);
    }

    private boolean tryCustomPortalBlock(Player player, Location to) {
        if (!plugin.getConfig().getBoolean("launchpads.teleporter-block.enabled", true)) {
            return false;
        }
        Material trigger = readTeleporterBlockMaterial();
        Block feet = to.getBlock();
        Block under = feet.getRelative(BlockFace.DOWN);
        if (feet.getType() != trigger && under.getType() != trigger) {
            return false;
        }
        Location fx = under.getType() == trigger ? under.getLocation().add(0.5, 1.0, 0.5) : feet.getLocation().add(0.5, 0.5, 0.5);
        player.getWorld().spawnParticle(Particle.PORTAL, fx, 45, 0.45, 0.35, 0.45, 0.07);
        player.getWorld().spawnParticle(Particle.ENCHANT, fx, 25, 0.35, 0.25, 0.35, 0.02);
        return teleportToConfiguredPortal(player);
    }

    private Material readTeleporterBlockMaterial() {
        String raw = plugin.getConfig().getString("launchpads.teleporter-block.material", "AMETHYST_BLOCK");
        if (raw == null || raw.isBlank()) {
            return Material.AMETHYST_BLOCK;
        }
        try {
            Material parsed = Material.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
            return parsed.isBlock() ? parsed : Material.AMETHYST_BLOCK;
        } catch (IllegalArgumentException ignored) {
            return Material.AMETHYST_BLOCK;
        }
    }

    private boolean teleportToConfiguredPortal(Player player) {
        long now = System.currentTimeMillis();
        if (now - portalCooldown.getOrDefault(player.getUniqueId(), 0L) < 1500L) {
            return false;
        }
        Location target = readEndPortalTarget();
        if (target == null || target.getWorld() == null) {
            player.sendActionBar(Component.text("EndPortal non configure.", NamedTextColor.RED));
            return false;
        }
        portalCooldown.put(player.getUniqueId(), now);
        player.teleport(target);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.8f, 1.15f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 55, 0.6, 0.8, 0.6, 0.1);
        launch(player, LaunchpadStyle.PARACHUTE);
        return true;
    }

    private Location readEndPortalTarget() {
        List<Location> targets = listEndPortalTargets();
        if (!targets.isEmpty()) {
            return targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        }
        String worldName = plugin.getConfig().getString("launchpads.endportal.target.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = plugin.getConfig().getDouble("launchpads.endportal.target.x", 0.0);
        double y = plugin.getConfig().getDouble("launchpads.endportal.target.y", 80.0);
        double z = plugin.getConfig().getDouble("launchpads.endportal.target.z", 0.0);
        float yaw = (float) plugin.getConfig().getDouble("launchpads.endportal.target.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("launchpads.endportal.target.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private static String encodePortalTarget(Location loc) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        return world + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    private static Location decodePortalTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] s = raw.split(";");
        if (s.length != 6) {
            return null;
        }
        World world = Bukkit.getWorld(s[0]);
        if (world == null) {
            return null;
        }
        try {
            double x = Double.parseDouble(s[1]);
            double y = Double.parseDouble(s[2]);
            double z = Double.parseDouble(s[3]);
            float yaw = Float.parseFloat(s[4]);
            float pitch = Float.parseFloat(s[5]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void launch(Player player, LaunchpadStyle style) {
        Vector look = player.getLocation().getDirection();
        Vector horiz = new Vector(look.getX(), 0, look.getZ());
        if (horiz.lengthSquared() < 1e-6) {
            horiz = new Vector(0, 0, 1);
        } else {
            horiz.normalize();
        }
        Vector v = horiz.multiply(style.horizontal()).setY(style.vertical());
        double max = style.isParachuteStyle()
                ? plugin.getConfig().getDouble("launchpads.parachute-max-velocity", 4.35)
                : plugin.getConfig().getDouble("launchpads.max-velocity", 3.8);
        if (v.length() > max) {
            v.normalize().multiply(max);
        }
        player.setVelocity(v);
        if (style.slowFallTicks() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, style.slowFallTicks(), 0, false, false, true));
        }
        Location p = player.getLocation().add(0, 0.1, 0);
        World world = player.getWorld();
        world.spawnParticle(Particle.CLOUD, p, 18, 0.35, 0.05, 0.35, 0.04);
        world.spawnParticle(Particle.HAPPY_VILLAGER, p, 8, 0.4, 0.1, 0.4, 0.02);
        player.playSound(p, Sound.ENTITY_SLIME_JUMP, style.soundVolume(), style.soundPitch());

        if (style.isParachuteStyle()) {
            parachuteFallImmuneUntil.put(player.getUniqueId(), System.currentTimeMillis() + 15_000L);
            world.spawnParticle(Particle.CLOUD, p.clone().add(0, 1.8, 0), 35, 0.9, 0.15, 0.9, 0.02);
            world.spawnParticle(Particle.SNOWFLAKE, p.clone().add(0, 1.5, 0), 22, 0.7, 0.2, 0.7, 0.03);
            player.playSound(p, Sound.ITEM_ELYTRA_FLYING, 0.55f, 1.15f);
            player.playSound(p, Sound.ENTITY_BAT_TAKEOFF, 0.35f, 0.85f);
            startParachuteCanopyFx(player);
        }
    }

    private void startParachuteCanopyFx(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask old = parachuteFxTasks.remove(uuid);
        if (old != null) {
            old.cancel();
        }
        int maxTicks = Math.max(40, plugin.getConfig().getInt("launchpads.parachute-fx-duration-ticks", 220));
        BukkitTask task = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    parachuteFxTasks.remove(uuid);
                    cancel();
                    return;
                }
                tick++;
                if (tick > maxTicks) {
                    parachuteFxTasks.remove(uuid);
                    cancel();
                    return;
                }
                boolean touchedBlock = player.getLocation().getBlock().getType().isSolid()
                        || player.getLocation().clone().subtract(0, 0.12, 0).getBlock().getType().isSolid();
                if (touchedBlock) {
                    parachuteFxTasks.remove(uuid);
                    cancel();
                    return;
                }
                applySneakDive(player);
                tickParachuteParticles(player, tick);
            }
        }.runTaskTimer(plugin, 2L, 3L);
        parachuteFxTasks.put(uuid, task);
    }

    private void applySneakDive(Player player) {
        if (!player.isSneaking()) {
            return;
        }
        Vector vel = player.getVelocity();
        double diveY = plugin.getConfig().getDouble("launchpads.parachute-sneak-dive-y", -0.62);
        double targetY = Math.min(diveY, vel.getY() - 0.08);
        vel.setY(targetY);
        player.setVelocity(vel);
    }

    private void tickParachuteParticles(Player player, int tick) {
        World w = player.getWorld();
        Location base = player.getLocation().add(0, 2.15, 0);
        double phase = tick * 0.22;

        for (int i = 0; i < 14; i++) {
            double a = phase + (Math.PI * 2 / 14) * i;
            double r = 1.05 + Math.sin(tick * 0.08) * 0.06;
            double x = Math.cos(a) * r;
            double z = Math.sin(a) * r;
            double wobble = Math.sin(phase * 1.3 + i * 0.4) * 0.06;
            w.spawnParticle(Particle.CLOUD, base.clone().add(x, wobble, z), 1, 0.02, 0.01, 0.02, 0);
        }

        for (int s = 0; s < 5; s++) {
            double a = phase * 0.8 + s * (Math.PI * 2 / 5);
            w.spawnParticle(Particle.WHITE_SMOKE,
                    base.clone().add(Math.cos(a) * 0.35, -0.55 - s * 0.12, Math.sin(a) * 0.35),
                    1, 0.04, 0.15, 0.04, 0.008);
        }

        w.spawnParticle(Particle.SNOWFLAKE, base.clone().add(0, -0.45, 0), 5, 0.55, 0.15, 0.55, 0.008);
        w.spawnParticle(Particle.END_ROD, base.clone().add(0, -0.2, 0), 2, 0.45, 0.08, 0.45, 0.002);
        if (cosmeticsManager != null) {
            Particle selected = cosmeticsManager.parachuteParticle(player.getUniqueId());
            if (selected != null) {
                w.spawnParticle(selected, base.clone().add(0, -0.1, 0), 6, 0.55, 0.12, 0.55, 0.01);
            }
        }

        if (tick % 4 == 0) {
            w.spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 0.3, 0), 2, 0.25, 0.1, 0.25, 0.01);
        }

        spawnParachuteParticleIfPresent(w, base.clone().add(0, -0.15, 0));

        if (tick % 10 == 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_WOOL_STEP, 0.08f, 1.4f + (tick % 7) * 0.02f);
        }
    }

    /** Particule parachute vanilla (1.20.5+) si disponible. */
    private static void spawnParachuteParticleIfPresent(World w, Location at) {
        try {
            Particle para = Particle.valueOf("PARACHUTE");
            w.spawnParticle(para, at, 2, 0.4, 0.06, 0.4, 0);
        } catch (IllegalArgumentException ignored) {
            w.spawnParticle(Particle.CLOUD, at, 3, 0.35, 0.05, 0.35, 0.01);
        }
    }
}
