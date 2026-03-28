package fr.dpmr.game;

import fr.dpmr.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Familiers visibles (tête = skin du joueur, armure cuir, arme en main) avec 5 rôles distincts.
 */
public class FamiliarPetManager implements Listener {

    private final JavaPlugin plugin;
    private final ZoneManager zoneManager;
    private final Map<UUID, PetState> pets = new HashMap<>();
    private BukkitTask task;

    private static final class PetState {
        final ArmorStand stand;
        final PetType type;
        long lastShotMs;
        long lastMedicPulseMs;
        long lastMeleeMs;

        PetState(ArmorStand stand, PetType type) {
            this.stand = stand;
            this.type = type;
            this.lastShotMs = 0L;
            this.lastMedicPulseMs = 0L;
            this.lastMeleeMs = 0L;
        }
    }

    public FamiliarPetManager(JavaPlugin plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID u : pets.keySet().toArray(UUID[]::new)) {
            dismiss(u);
        }
    }

    public boolean has(UUID owner) {
        PetState s = pets.get(owner);
        return s != null && s.stand.isValid();
    }

    public void summon(Player owner, PetType type) {
        if (type == null) {
            owner.sendMessage(Component.text("Type de familier inconnu.", NamedTextColor.RED));
            return;
        }
        if (has(owner.getUniqueId())) {
            dismiss(owner.getUniqueId());
        }
        Location spawn = owner.getLocation().add(0.6, 0, 0.6);
        ArmorStand as = owner.getWorld().spawn(spawn, ArmorStand.class);
        as.setInvisible(false);
        as.setMarker(false);
        as.setSmall(true);
        as.setArms(true);
        as.setBasePlate(false);
        as.setGravity(false);
        as.setCanPickupItems(false);
        as.setInvulnerable(true);
        as.setPersistent(true);
        as.setCustomNameVisible(true);
        as.customName(Component.text(type.displayFr(), type.nameColor()));
        applySkinAndGear(owner, as, type);
        pets.put(owner.getUniqueId(), new PetState(as, type));
        owner.playSound(owner.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 0.6f, 1.25f);
        owner.sendMessage(Component.text("Familier invoque : " + type.displayFr(), NamedTextColor.GREEN));
    }

    private void applySkinAndGear(Player owner, ArmorStand as, PetType type) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(owner);
        head.setItemMeta(sm);
        var eq = as.getEquipment();
        eq.setHelmet(head);
        eq.setChestplate(type.leatherPiece(Material.LEATHER_CHESTPLATE));
        eq.setLeggings(type.leatherPiece(Material.LEATHER_LEGGINGS));
        eq.setBoots(type.leatherPiece(Material.LEATHER_BOOTS));
        eq.setItemInMainHand(type.createMainHand());
        ItemStack off = type.createOffHand();
        eq.setItemInOffHand(off != null ? off : new ItemStack(Material.AIR));
    }

    public void dismiss(UUID owner) {
        PetState s = pets.remove(owner);
        if (s != null && s.stand.isValid()) {
            s.stand.remove();
        }
    }

    private void tick() {
        for (Map.Entry<UUID, PetState> e : new HashMap<>(pets).entrySet()) {
            UUID ownerId = e.getKey();
            PetState state = e.getValue();
            Player owner = Bukkit.getPlayer(ownerId);
            if (owner == null || !owner.isOnline() || state.stand == null || !state.stand.isValid()) {
                dismiss(ownerId);
                continue;
            }
            follow(owner, state.stand);
            switch (state.type) {
                case GUNNER -> behaveGunner(owner, state);
                case MEDIC -> behaveMedic(owner, state);
                case SNIPER -> behaveSniper(owner, state);
                case SCOUT -> behaveScout(owner, state);
                case BRUTE -> behaveBrute(owner, state);
            }
        }
    }

    private void follow(Player owner, ArmorStand pet) {
        Location o = owner.getLocation();
        Location p = pet.getLocation();
        double distSq = o.distanceSquared(p);
        Vector back = owner.getLocation().getDirection().clone().setY(0);
        if (back.lengthSquared() < 0.01) {
            back = new Vector(0, 0, -1);
        } else {
            back.normalize().multiply(-1.0);
        }
        Location target = o.clone().add(back.multiply(1.05)).add(0, 0.15, 0);
        target.setYaw(o.getYaw());
        target.setPitch(0);

        if (distSq > 20 * 20) {
            pet.teleport(target);
            return;
        }
        if (distSq > 3.5 * 3.5) {
            Vector v = target.toVector().subtract(p.toVector()).multiply(0.35);
            pet.teleport(p.add(v));
        } else {
            pet.teleport(target);
        }
    }

    private boolean combatOk(Player owner, Location at) {
        return zoneManager == null || zoneManager.isCombatAllowed(at);
    }

    private void face(ArmorStand pet, Location point) {
        Location eye = pet.getEyeLocation();
        Vector to = point.toVector().subtract(eye.toVector());
        if (to.lengthSquared() < 0.0001) {
            return;
        }
        Location L = eye.clone();
        L.setDirection(to);
        pet.setRotation(L.getYaw(), 0);
    }

    private void behaveGunner(Player owner, PetState state) {
        if (!combatOk(owner, owner.getLocation())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - state.lastShotMs < 220) {
            return;
        }
        LivingEntity target = findEnemyPlayer(owner, state.stand.getEyeLocation(), 18.0);
        if (target == null || !combatOk(owner, target.getLocation())) {
            return;
        }
        state.lastShotMs = now;
        face(state.stand, target.getEyeLocation());
        shootHitscan(owner, state.stand, state.stand.getEyeLocation(), target, 18.0, 6.1, 2.2);
    }

    private void behaveMedic(Player owner, PetState state) {
        long now = System.currentTimeMillis();
        if (now - state.lastMedicPulseMs < 2000) {
            return;
        }
        state.lastMedicPulseMs = now;
        Location c = state.stand.getLocation().add(0, 0.5, 0);
        World w = c.getWorld();
        if (w == null) {
            return;
        }
        w.playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 0.35f, 1.6f);
        w.spawnParticle(Particle.HEART, c, 6, 0.5, 0.35, 0.5, 0.02);
        double r = 4.0;
        for (Player p : w.getPlayers()) {
            if (!p.isValid() || p.isDead()) {
                continue;
            }
            if (p.getLocation().distanceSquared(c) > r * r) {
                continue;
            }
            double max = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            p.setHealth(Math.min(max, p.getHealth() + 1.0));
        }
    }

    private void behaveSniper(Player owner, PetState state) {
        if (!combatOk(owner, owner.getLocation())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - state.lastShotMs < 780) {
            return;
        }
        LivingEntity target = findEnemyPlayer(owner, state.stand.getEyeLocation(), 28.0);
        if (target == null || !combatOk(owner, target.getLocation())) {
            return;
        }
        state.lastShotMs = now;
        face(state.stand, target.getEyeLocation());
        shootHitscan(owner, state.stand, state.stand.getEyeLocation(), target, 28.0, 10.2, 0.9);
    }

    private void behaveScout(Player owner, PetState state) {
        if (!combatOk(owner, owner.getLocation())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - state.lastShotMs < 140) {
            return;
        }
        LivingEntity target = findEnemyPlayer(owner, state.stand.getEyeLocation(), 14.0);
        if (target == null || !combatOk(owner, target.getLocation())) {
            return;
        }
        state.lastShotMs = now;
        face(state.stand, target.getEyeLocation());
        shootHitscan(owner, state.stand, state.stand.getEyeLocation(), target, 14.0, 3.4, 3.8);
    }

    private void behaveBrute(Player owner, PetState state) {
        if (!combatOk(owner, owner.getLocation())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - state.lastMeleeMs < 400) {
            return;
        }
        LivingEntity target = findEnemyPlayer(owner, state.stand.getLocation(), 4.0);
        if (target == null || !combatOk(owner, target.getLocation())) {
            return;
        }
        if (target.getLocation().distanceSquared(state.stand.getLocation()) > 2.7 * 2.7) {
            return;
        }
        state.lastMeleeMs = now;
        face(state.stand, target.getLocation().add(0, 1, 0));
        World w = state.stand.getWorld();
        double dmg = 8.2 * ThreadLocalRandom.current().nextDouble(0.92, 1.08);
        boolean lethal = target instanceof Player victim && victim.getHealth() <= dmg;
        target.damage(dmg, owner);
        if (lethal && target instanceof Player victim) {
            announcePetKill(owner, victim);
        }
        w.playSound(state.stand.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.55f, 0.85f);
        w.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 14, 0.2, 0.35, 0.2, 0.02);
    }

    private LivingEntity findEnemyPlayer(Player owner, Location from, double range) {
        World w = from.getWorld();
        if (w == null) {
            return null;
        }
        LivingEntity best = null;
        double bestD = range * range;
        for (Player p : w.getPlayers()) {
            if (p == owner || !p.isValid() || p.isDead()) {
                continue;
            }
            double d = p.getLocation().distanceSquared(from);
            if (d > bestD) {
                continue;
            }
            if (!owner.hasLineOfSight(p)) {
                continue;
            }
            bestD = d;
            best = p;
        }
        return best;
    }

    private void shootHitscan(Player owner, ArmorStand shooter, Location origin, LivingEntity target,
                              double range, double baseDmg, double spreadDeg) {
        World w = origin.getWorld();
        if (w == null) {
            return;
        }
        Vector dir = target.getEyeLocation().toVector().subtract(origin.toVector()).normalize();
        dir = applySpread(origin, dir, spreadDeg);

        RayTraceResult hit = w.rayTraceEntities(origin, dir, range, 0.35, ent -> {
            if (!(ent instanceof Player p) || p == owner || !p.isValid() || p.isDead()) {
                return false;
            }
            GameMode gm = p.getGameMode();
            return gm != GameMode.SPECTATOR && gm != GameMode.CREATIVE;
        });

        drawBeam(w, origin, dir, range);
        w.playSound(origin, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.35f, 1.75f);

        if (hit != null && hit.getHitEntity() instanceof LivingEntity living) {
            double dmg = baseDmg * ThreadLocalRandom.current().nextDouble(0.9, 1.1);
            boolean lethal = living instanceof Player victim && victim.getHealth() <= dmg;
            living.damage(dmg, owner);
            if (lethal && living instanceof Player victim) {
                announcePetKill(owner, victim);
            }
            w.spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1, 0), 4, 0.15, 0.25, 0.15, 0.01);
        }
    }

    private void announcePetKill(Player owner, Player victim) {
        Bukkit.broadcast(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text(victim.getName(), NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD))
                .append(Component.text(" a ete elimine par le familier de ", NamedTextColor.GRAY))
                .append(Component.text(owner.getName(), NamedTextColor.AQUA, net.kyori.adventure.text.format.TextDecoration.BOLD))
                .append(Component.text(" ✧", NamedTextColor.GOLD)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPetDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) {
            return;
        }
        for (PetState pet : pets.values()) {
            if (pet.stand.getUniqueId().equals(stand.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private static Vector applySpread(Location eye, Vector baseDir, double maxDegrees) {
        if (maxDegrees <= 0.001) {
            return baseDir.clone().normalize();
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location L = eye.clone();
        L.setDirection(baseDir);
        float yawOff = (float) ((r.nextDouble() * 2 - 1) * maxDegrees);
        float pitchOff = (float) ((r.nextDouble() * 2 - 1) * maxDegrees * 0.62);
        L.setYaw(L.getYaw() + yawOff);
        float np = L.getPitch() + pitchOff;
        np = Math.max(-89.5f, Math.min(89.5f, np));
        L.setPitch(np);
        return L.getDirection().normalize();
    }

    private static void drawBeam(World world, Location start, Vector direction, double range) {
        Vector step = direction.clone().normalize().multiply(0.45);
        Location cursor = start.clone();
        int max = Math.max(1, (int) (range / 0.45));
        for (int i = 0; i < max; i++) {
            world.spawnParticle(Particle.SMOKE, cursor, 1, 0.02, 0.02, 0.02, 0.001);
            cursor.add(step);
        }
    }
}
