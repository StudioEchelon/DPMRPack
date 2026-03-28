package fr.dpmr.zone;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.HeightMap;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ZoneManager implements Listener {

    private final JavaPlugin plugin;
    private final org.bukkit.NamespacedKey keyZoneWandSel;
    private final Map<UUID, ZoneView> lastZoneView = new HashMap<>();
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final Map<UUID, List<Location>> polySelections = new HashMap<>();

    private static final class Selection {
        Location pos1;
        Location pos2;
    }

    private enum ZoneView {
        SAFE,
        PVP,
        NEUTRAL
    }

    public ZoneManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.keyZoneWandSel = new org.bukkit.NamespacedKey(plugin, "dpmr_zone_wand_sel");
    }

    public void setSafeZone(Player player, double radius) {
        Location loc = player.getLocation();
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("zones.safe.shape", "sphere");
        cfg.set("zones.safe.enabled", true);
        cfg.set("zones.safe.world", loc.getWorld().getName());
        cfg.set("zones.safe.x", loc.getX());
        cfg.set("zones.safe.y", loc.getY());
        cfg.set("zones.safe.z", loc.getZ());
        cfg.set("zones.safe.radius", Math.max(1.0, radius));
        plugin.saveConfig();
        player.sendMessage(Component.text("Safe zone definie (r=" + Math.round(radius) + ")", NamedTextColor.GREEN));
    }

    public void setWarZone(Player player, double radius) {
        Location loc = player.getLocation();
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("zones.war.shape", "sphere");
        cfg.set("zones.war.enabled", true);
        cfg.set("zones.war.world", loc.getWorld().getName());
        cfg.set("zones.war.x", loc.getX());
        cfg.set("zones.war.y", loc.getY());
        cfg.set("zones.war.z", loc.getZ());
        cfg.set("zones.war.radius", Math.max(1.0, radius));
        plugin.saveConfig();
        player.sendMessage(Component.text("War zone definie (r=" + Math.round(radius) + ")", NamedTextColor.GOLD));
    }

    public boolean setSafeZoneFromSelection(Player player) {
        Selection sel = selections.get(player.getUniqueId());
        if (sel == null || sel.pos1 == null || sel.pos2 == null) {
            player.sendMessage(Component.text("Selection incomplete: define pos1 et pos2 avec la wand.", NamedTextColor.RED));
            return false;
        }
        if (sel.pos1.getWorld() == null || sel.pos2.getWorld() == null || sel.pos1.getWorld() != sel.pos2.getWorld()) {
            player.sendMessage(Component.text("Pos1 et Pos2 doivent etre dans le meme monde.", NamedTextColor.RED));
            return false;
        }
        saveCuboidZone("safe", sel.pos1, sel.pos2);
        plugin.saveConfig();
        player.sendMessage(Component.text("Safe zone cubique definie depuis la selection.", NamedTextColor.GREEN));
        return true;
    }

    public boolean setWarZoneFromSelection(Player player) {
        Selection sel = selections.get(player.getUniqueId());
        if (sel == null || sel.pos1 == null || sel.pos2 == null) {
            player.sendMessage(Component.text("Selection incomplete: define pos1 et pos2 avec la wand.", NamedTextColor.RED));
            return false;
        }
        if (sel.pos1.getWorld() == null || sel.pos2.getWorld() == null || sel.pos1.getWorld() != sel.pos2.getWorld()) {
            player.sendMessage(Component.text("Pos1 et Pos2 doivent etre dans le meme monde.", NamedTextColor.RED));
            return false;
        }
        saveCuboidZone("war", sel.pos1, sel.pos2);
        plugin.saveConfig();
        player.sendMessage(Component.text("Zone PVP cubique definie depuis la selection.", NamedTextColor.GOLD));
        return true;
    }

    public void setSafeZoneAt(Player player, Location loc, double radius) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("zones.safe.shape", "sphere");
        cfg.set("zones.safe.enabled", true);
        cfg.set("zones.safe.world", loc.getWorld().getName());
        cfg.set("zones.safe.x", loc.getX());
        cfg.set("zones.safe.y", loc.getY());
        cfg.set("zones.safe.z", loc.getZ());
        cfg.set("zones.safe.radius", Math.max(1.0, radius));
        plugin.saveConfig();
        player.sendMessage(Component.text("Safe zone definie (r=" + Math.round(radius) + ")", NamedTextColor.GREEN));
    }

    public void setWarZoneAt(Player player, Location loc, double radius) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("zones.war.shape", "sphere");
        cfg.set("zones.war.enabled", true);
        cfg.set("zones.war.world", loc.getWorld().getName());
        cfg.set("zones.war.x", loc.getX());
        cfg.set("zones.war.y", loc.getY());
        cfg.set("zones.war.z", loc.getZ());
        cfg.set("zones.war.radius", Math.max(1.0, radius));
        plugin.saveConfig();
        player.sendMessage(Component.text("Zone PVP definie (r=" + Math.round(radius) + ")", NamedTextColor.GOLD));
    }

    public void deleteSafeZone(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("zones.safe.enabled", false);
        plugin.saveConfig();
        player.sendMessage(Component.text("Safe zone supprimee.", NamedTextColor.YELLOW));
    }

    public void deleteWarZone(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("zones.war.enabled", false);
        plugin.saveConfig();
        player.sendMessage(Component.text("Zone PVP supprimee.", NamedTextColor.YELLOW));
    }

    public ItemStack createZoneWand(String type, double radius) {
        ItemStack it = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text("Wand Zones (points + WorldEdit)", NamedTextColor.AQUA));
        meta.lore(java.util.List.of(
                Component.text("Clic gauche bloc: Pos1", NamedTextColor.GRAY),
                Component.text("Clic droit bloc: Pos2 + ajoute un point", NamedTextColor.GRAY),
                Component.text("Sneak + clic droit: clear points", NamedTextColor.YELLOW),
                Component.text("/dpmr zone <safe|war> setpoly", NamedTextColor.DARK_AQUA),
                Component.text("/dpmr zone clearpoints", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(keyZoneWandSel, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }

    public void clearPolyPoints(Player player) {
        polySelections.remove(player.getUniqueId());
        player.sendMessage(Component.text("Points de zone effaces.", NamedTextColor.YELLOW));
    }

    public boolean setSafeZoneFromPoly(Player player) {
        return setZoneFromPoly("safe", player, NamedTextColor.GREEN, "Safe zone polygonale definie.");
    }

    public boolean setWarZoneFromPoly(Player player) {
        return setZoneFromPoly("war", player, NamedTextColor.GOLD, "Zone PVP polygonale definie.");
    }

    public int getPolyPointCount(Player player) {
        return polySelections.getOrDefault(player.getUniqueId(), List.of()).size();
    }

    public boolean isInSafeZone(Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("zones.safe.enabled", false)) {
            return false;
        }
        String w = cfg.getString("zones.safe.world", "");
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(w)) {
            return false;
        }
        if (isCuboid(cfg, "safe")) {
            return insideCuboid(cfg, "safe", loc);
        }
        if (isPolygon(cfg, "safe")) {
            return insidePolygon(cfg, "safe", loc);
        }
        double x = cfg.getDouble("zones.safe.x", 0);
        double y = cfg.getDouble("zones.safe.y", 0);
        double z = cfg.getDouble("zones.safe.z", 0);
        double r = Math.max(0, cfg.getDouble("zones.safe.radius", 0));
        return loc.distanceSquared(new Location(loc.getWorld(), x, y, z)) <= r * r;
    }

    /**
     * Si war zone activée: combat autorisé uniquement dedans.
     * Sinon: combat autorisé partout sauf safe zone.
     */
    public boolean isCombatAllowed(Location loc) {
        if (isInSafeZone(loc)) {
            return false;
        }
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("zones.war.enabled", false)) {
            return true;
        }
        return isInsideWarRegion(loc);
    }

    /**
     * {@code true} si la position est dans la région war (monde + forme), sans tenir compte de la safe.
     */
    public boolean isInsideWarRegion(Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("zones.war.enabled", false)) {
            return false;
        }
        String w = cfg.getString("zones.war.world", "");
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(w)) {
            return false;
        }
        if (isCuboid(cfg, "war")) {
            return insideCuboid(cfg, "war", loc);
        }
        if (isPolygon(cfg, "war")) {
            return insidePolygon(cfg, "war", loc);
        }
        double x = cfg.getDouble("zones.war.x", 0);
        double y = cfg.getDouble("zones.war.y", 0);
        double z = cfg.getDouble("zones.war.z", 0);
        double r = Math.max(0, cfg.getDouble("zones.war.radius", 0));
        return loc.distanceSquared(new Location(loc.getWorld(), x, y, z)) <= r * r;
    }

    /**
     * Pieds du joueur sur surface solide, dans la war zone (air aux pieds + tête).
     */
    public Location pickRandomSurfaceFeetInWarZone(int maxAttempts) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("zones.war.enabled", false)) {
            return null;
        }
        String worldName = cfg.getString("zones.war.world", "");
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < maxAttempts; i++) {
            Location probe = randomXZProbeInWar(world, cfg, rnd);
            if (probe == null) {
                continue;
            }
            int y = world.getHighestBlockYAt(probe.getBlockX(), probe.getBlockZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight() - 1;
            if (isCuboid(cfg, "war") || isPolygon(cfg, "war")) {
                minY = cfg.getInt("zones.war.min.y", minY);
                maxY = cfg.getInt("zones.war.max.y", maxY);
            }
            if (y < minY || y > maxY) {
                continue;
            }
            Location feet = new Location(world, probe.getBlockX() + 0.5, y + 1.0, probe.getBlockZ() + 0.5);
            if (!feet.getBlock().getType().isAir()) {
                continue;
            }
            if (!feet.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                continue;
            }
            if (!feet.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                continue;
            }
            if (!isInsideWarRegion(feet)) {
                continue;
            }
            return feet;
        }
        return null;
    }

    /**
     * Hors safe : autour du spawn vanilla du monde (si war désactivée).
     */
    public Location pickRandomSurfaceNearWorldSpawn(String worldName, int spreadBlocks, int maxAttempts) {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        Location spawn = world.getSpawnLocation();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int spread = Math.max(8, spreadBlocks);
        for (int i = 0; i < maxAttempts; i++) {
            int x = spawn.getBlockX() + rnd.nextInt(-spread, spread + 1);
            int z = spawn.getBlockZ() + rnd.nextInt(-spread, spread + 1);
            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Location feet = new Location(world, x + 0.5, y + 1.0, z + 0.5);
            if (isInSafeZone(feet)) {
                continue;
            }
            if (!feet.getBlock().getType().isAir() || !feet.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                continue;
            }
            if (!feet.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                continue;
            }
            return feet;
        }
        return null;
    }

    private Location randomXZProbeInWar(World world, FileConfiguration cfg, ThreadLocalRandom rnd) {
        if (isCuboid(cfg, "war")) {
            int minX = cfg.getInt("zones.war.min.x");
            int maxX = cfg.getInt("zones.war.max.x");
            int minZ = cfg.getInt("zones.war.min.z");
            int maxZ = cfg.getInt("zones.war.max.z");
            int xa = Math.min(minX, maxX);
            int xb = Math.max(minX, maxX);
            int za = Math.min(minZ, maxZ);
            int zb = Math.max(minZ, maxZ);
            int x = xa == xb ? xa : rnd.nextInt(xa, xb + 1);
            int z = za == zb ? za : rnd.nextInt(za, zb + 1);
            return new Location(world, x, 0, z);
        }
        if (isPolygon(cfg, "war")) {
            List<String> raw = cfg.getStringList("zones.war.points");
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (String s : raw) {
                String[] sp = s.split(";");
                if (sp.length != 2) {
                    continue;
                }
                try {
                    int px = Integer.parseInt(sp[0]);
                    int pz = Integer.parseInt(sp[1]);
                    minX = Math.min(minX, px);
                    maxX = Math.max(maxX, px);
                    minZ = Math.min(minZ, pz);
                    maxZ = Math.max(maxZ, pz);
                } catch (NumberFormatException ignored) {
                }
            }
            if (minX == Integer.MAX_VALUE) {
                return null;
            }
            for (int t = 0; t < 12; t++) {
                int x = minX == maxX ? minX : rnd.nextInt(minX, maxX + 1);
                int z = minZ == maxZ ? minZ : rnd.nextInt(minZ, maxZ + 1);
                Location test = new Location(world, x + 0.5, 64, z + 0.5);
                if (insidePolygon(cfg, "war", test)) {
                    return new Location(world, x, 0, z);
                }
            }
            return null;
        }
        double cx = cfg.getDouble("zones.war.x", 0);
        double cz = cfg.getDouble("zones.war.z", 0);
        double r = Math.max(1, cfg.getDouble("zones.war.radius", 32));
        for (int t = 0; t < 8; t++) {
            double u = rnd.nextDouble();
            double v = rnd.nextDouble();
            double dx = (u * 2 - 1) * r;
            double dz = (v * 2 - 1) * r;
            if (dx * dx + dz * dz <= r * r) {
                return new Location(world, cx + dx, 0, cz + dz);
            }
        }
        return null;
    }

    public void describeTo(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        player.sendMessage(Component.text("Zones:", NamedTextColor.AQUA));
        player.sendMessage(Component.text(" - safe: " + (cfg.getBoolean("zones.safe.enabled", false) ? "ON" : "OFF"), NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - war: " + (cfg.getBoolean("zones.war.enabled", false) ? "ON" : "OFF"), NamedTextColor.GRAY));
    }

    @EventHandler
    public void onZoneWandUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        Byte isSelWand = meta.getPersistentDataContainer().get(keyZoneWandSel, PersistentDataType.BYTE);
        if (isSelWand == null || isSelWand != 1) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("dpmr.admin")) {
            player.sendMessage(Component.text("Pas la permission dpmr.admin", NamedTextColor.RED));
            return;
        }
        event.setCancelled(true);
        if (event.getClickedBlock() == null) {
            return;
        }
        Location at = event.getClickedBlock().getLocation();
        Selection sel = selections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
        if (action == Action.LEFT_CLICK_BLOCK) {
            sel.pos1 = at;
            player.sendMessage(Component.text("Pos1 = " + fmt(at), NamedTextColor.GREEN));
        } else {
            sel.pos2 = at;
            player.sendMessage(Component.text("Pos2 = " + fmt(at), NamedTextColor.GOLD));
            if (player.isSneaking()) {
                clearPolyPoints(player);
                return;
            }
            List<Location> pts = polySelections.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
            pts.add(at);
            player.sendActionBar(Component.text("Point ajoute (#" + pts.size() + ")", NamedTextColor.AQUA));
        }
    }

    @EventHandler
    public void onMoveTitle(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()
                && from.getWorld() == to.getWorld()) {
            return;
        }
        Player p = event.getPlayer();
        ZoneView now = currentZone(to);
        ZoneView prev = lastZoneView.getOrDefault(p.getUniqueId(), currentZone(from));
        if (now == prev) {
            return;
        }
        lastZoneView.put(p.getUniqueId(), now);
        Title.Times times = Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1100), Duration.ofMillis(300));
        switch (now) {
            case SAFE -> p.showTitle(Title.title(
                    Component.text("ZONE SAFE", NamedTextColor.GREEN),
                    Component.text("PVP desactive", NamedTextColor.GRAY),
                    times
            ));
            case PVP -> p.showTitle(Title.title(
                    Component.text("ZONE PVP", NamedTextColor.RED),
                    Component.text("Combat autorise", NamedTextColor.GOLD),
                    times
            ));
            case NEUTRAL -> p.showTitle(Title.title(
                    Component.text("ZONE NEUTRE", NamedTextColor.YELLOW),
                    Component.text("Standard", NamedTextColor.GRAY),
                    times
            ));
        }
    }

    private ZoneView currentZone(Location loc) {
        if (isInSafeZone(loc)) {
            return ZoneView.SAFE;
        }
        if (isCombatAllowed(loc)) {
            return ZoneView.PVP;
        }
        return ZoneView.NEUTRAL;
    }

    private static String fmt(Location at) {
        return at.getWorld().getName() + " " + at.getBlockX() + "," + at.getBlockY() + "," + at.getBlockZ();
    }

    private void saveCuboidZone(String key, Location a, Location b) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("zones." + key + ".enabled", true);
        cfg.set("zones." + key + ".shape", "cuboid");
        cfg.set("zones." + key + ".world", a.getWorld().getName());
        cfg.set("zones." + key + ".min.x", Math.min(a.getBlockX(), b.getBlockX()));
        cfg.set("zones." + key + ".min.y", Math.min(a.getBlockY(), b.getBlockY()));
        cfg.set("zones." + key + ".min.z", Math.min(a.getBlockZ(), b.getBlockZ()));
        cfg.set("zones." + key + ".max.x", Math.max(a.getBlockX(), b.getBlockX()));
        cfg.set("zones." + key + ".max.y", Math.max(a.getBlockY(), b.getBlockY()));
        cfg.set("zones." + key + ".max.z", Math.max(a.getBlockZ(), b.getBlockZ()));
    }

    private boolean isCuboid(FileConfiguration cfg, String key) {
        return "cuboid".equalsIgnoreCase(cfg.getString("zones." + key + ".shape", "sphere"));
    }

    private boolean isPolygon(FileConfiguration cfg, String key) {
        return "polygon".equalsIgnoreCase(cfg.getString("zones." + key + ".shape", "sphere"));
    }

    private boolean insideCuboid(FileConfiguration cfg, String key, Location loc) {
        int minX = cfg.getInt("zones." + key + ".min.x", Integer.MIN_VALUE);
        int minY = cfg.getInt("zones." + key + ".min.y", Integer.MIN_VALUE);
        int minZ = cfg.getInt("zones." + key + ".min.z", Integer.MIN_VALUE);
        int maxX = cfg.getInt("zones." + key + ".max.x", Integer.MAX_VALUE);
        int maxY = cfg.getInt("zones." + key + ".max.y", Integer.MAX_VALUE);
        int maxZ = cfg.getInt("zones." + key + ".max.z", Integer.MAX_VALUE);
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private boolean setZoneFromPoly(String key, Player player, NamedTextColor color, String okMsg) {
        List<Location> pts = polySelections.get(player.getUniqueId());
        if (pts == null || pts.size() < 3) {
            player.sendMessage(Component.text("Il faut au moins 3 points (wand clic droit).", NamedTextColor.RED));
            return false;
        }
        String world = pts.get(0).getWorld() != null ? pts.get(0).getWorld().getName() : null;
        if (world == null) {
            player.sendMessage(Component.text("Monde invalide.", NamedTextColor.RED));
            return false;
        }
        for (Location p : pts) {
            if (p.getWorld() == null || !world.equals(p.getWorld().getName())) {
                player.sendMessage(Component.text("Tous les points doivent etre dans le meme monde.", NamedTextColor.RED));
                return false;
            }
        }
        int minY = pts.stream().mapToInt(Location::getBlockY).min().orElse(player.getLocation().getBlockY());
        int maxY = pts.stream().mapToInt(Location::getBlockY).max().orElse(player.getLocation().getBlockY()) + 5;
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("zones." + key + ".enabled", true);
        cfg.set("zones." + key + ".shape", "polygon");
        cfg.set("zones." + key + ".world", world);
        cfg.set("zones." + key + ".min.y", minY);
        cfg.set("zones." + key + ".max.y", maxY);
        List<String> raw = new ArrayList<>();
        for (Location p : pts) {
            raw.add(p.getBlockX() + ";" + p.getBlockZ());
        }
        cfg.set("zones." + key + ".points", raw);
        plugin.saveConfig();
        player.sendMessage(Component.text(okMsg + " (" + pts.size() + " points)", color));
        return true;
    }

    private boolean insidePolygon(FileConfiguration cfg, String key, Location loc) {
        int y = loc.getBlockY();
        int minY = cfg.getInt("zones." + key + ".min.y", Integer.MIN_VALUE);
        int maxY = cfg.getInt("zones." + key + ".max.y", Integer.MAX_VALUE);
        if (y < minY || y > maxY) {
            return false;
        }
        List<String> raw = cfg.getStringList("zones." + key + ".points");
        if (raw.size() < 3) {
            return false;
        }
        List<int[]> pts = new ArrayList<>();
        for (String s : raw) {
            String[] sp = s.split(";");
            if (sp.length != 2) {
                continue;
            }
            try {
                pts.add(new int[]{Integer.parseInt(sp[0]), Integer.parseInt(sp[1])});
            } catch (NumberFormatException ignored) {
            }
        }
        if (pts.size() < 3) {
            return false;
        }
        double x = loc.getX();
        double z = loc.getZ();
        boolean inside = false;
        for (int i = 0, j = pts.size() - 1; i < pts.size(); j = i++) {
            double xi = pts.get(i)[0];
            double zi = pts.get(i)[1];
            double xj = pts.get(j)[0];
            double zj = pts.get(j)[1];
            boolean intersect = ((zi > z) != (zj > z))
                    && (x < (xj - xi) * (z - zi) / ((zj - zi) + 1e-9) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }
}

