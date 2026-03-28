package fr.dpmr.crate;

import fr.dpmr.data.PointsManager;
import fr.dpmr.game.BandageManager;
import fr.dpmr.game.DpmrConsumable;
import fr.dpmr.game.WeaponManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class CrateManager implements Listener {

    private final JavaPlugin plugin;
    private final PointsManager pointsManager;
    private final WeaponManager weaponManager;
    private final BandageManager bandageManager;
    private final File file;
    private final YamlConfiguration yaml;
    private final org.bukkit.NamespacedKey keyCrateKeyType;
    private final Map<String, CrateDef> crates = new HashMap<>();
    private final Set<UUID> opening = new HashSet<>();

    private record CrateDef(String id, String world, int x, int y, int z) {}

    private interface Reward {
        ItemStack preview();
        void give(Player player);
        String label();
        int weight();
    }

    public CrateManager(JavaPlugin plugin, PointsManager pointsManager, WeaponManager weaponManager, BandageManager bandageManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.weaponManager = weaponManager;
        this.bandageManager = bandageManager;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        this.keyCrateKeyType = new org.bukkit.NamespacedKey(plugin, "dpmr_crate_key_type");
        load();
    }

    private void load() {
        crates.clear();
        if (!yaml.isConfigurationSection("crates")) {
            return;
        }
        for (String id : Objects.requireNonNull(yaml.getConfigurationSection("crates")).getKeys(false)) {
            String base = "crates." + id + ".";
            String world = yaml.getString(base + "world", "");
            int x = yaml.getInt(base + "x", 0);
            int y = yaml.getInt(base + "y", 0);
            int z = yaml.getInt(base + "z", 0);
            if (!world.isBlank()) {
                crates.put(id.toLowerCase(Locale.ROOT), new CrateDef(id.toLowerCase(Locale.ROOT), world, x, y, z));
            }
        }
    }

    public void save() {
        yaml.set("crates", null);
        for (CrateDef def : crates.values()) {
            String base = "crates." + def.id + ".";
            yaml.set(base + "world", def.world);
            yaml.set(base + "x", def.x);
            yaml.set(base + "y", def.y);
            yaml.set(base + "z", def.z);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder crates.yml: " + e.getMessage());
        }
    }

    public void createCrate(Player admin, String id, Location at) {
        if (at.getWorld() == null) {
            return;
        }
        String k = id.toLowerCase(Locale.ROOT);
        crates.put(k, new CrateDef(k, at.getWorld().getName(), at.getBlockX(), at.getBlockY(), at.getBlockZ()));
        save();
        admin.sendMessage(Component.text("Caisse '" + k + "' creee.", NamedTextColor.GREEN));
    }

    public void deleteCrate(Player admin, String id) {
        String k = id.toLowerCase(Locale.ROOT);
        if (crates.remove(k) == null) {
            admin.sendMessage(Component.text("Caisse introuvable.", NamedTextColor.RED));
            return;
        }
        save();
        admin.sendMessage(Component.text("Caisse '" + k + "' supprimee.", NamedTextColor.YELLOW));
    }

    public List<String> listCrates() {
        return crates.keySet().stream().sorted().toList();
    }

    public ItemStack createKey(String crateId, int amount) {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, Math.max(1, amount));
        ItemMeta meta = key.getItemMeta();
        meta.displayName(Component.text("Cle de caisse: " + crateId, NamedTextColor.GOLD));
        meta.lore(List.of(Component.text("Utilise sur la caisse " + crateId, NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(keyCrateKeyType, PersistentDataType.STRING, crateId.toLowerCase(Locale.ROOT));
        key.setItemMeta(meta);
        return key;
    }

    private String readKeyType(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(keyCrateKeyType, PersistentDataType.STRING);
    }

    @EventHandler
    public void onUseCrate(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Player player = event.getPlayer();
        if (opening.contains(player.getUniqueId())) {
            return;
        }
        CrateDef def = crateAt(event.getClickedBlock().getLocation());
        if (def == null) {
            return;
        }
        String keyType = readKeyType(event.getItem());
        if (keyType == null || !keyType.equals(def.id)) {
            player.sendActionBar(Component.text("Il faut une cle '" + def.id + "'", NamedTextColor.RED));
            return;
        }
        event.setCancelled(true);
        consumeOneKey(player);
        openAnimation(player, def, event.getClickedBlock().getLocation().add(0.5, 1.15, 0.5));
    }

    private CrateDef crateAt(Location loc) {
        for (CrateDef def : crates.values()) {
            if (loc.getWorld() == null || !def.world.equals(loc.getWorld().getName())) {
                continue;
            }
            if (def.x == loc.getBlockX() && def.y == loc.getBlockY() && def.z == loc.getBlockZ()) {
                return def;
            }
        }
        return null;
    }

    private void consumeOneKey(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
    }

    private void openAnimation(Player player, CrateDef def, Location center) {
        opening.add(player.getUniqueId());
        List<Reward> pool = rewardPool(def.id);
        if (pool.isEmpty()) {
            opening.remove(player.getUniqueId());
            return;
        }
        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.001) {
            dir = new Vector(1, 0, 0);
        }
        dir.normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        List<ArmorStand> reels = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            Location at = center.clone().add(right.clone().multiply(i * 0.55));
            ArmorStand as = center.getWorld().spawn(at, ArmorStand.class);
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.getEquipment().setHelmet(randomReward(pool).preview());
            reels.add(as);
        }

        final int[] tick = {0};
        final Reward[] result = {randomWeighted(pool)};
        final BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick[0]++;
            int speed = tick[0] < 25 ? 1 : (tick[0] < 45 ? 2 : 4);
            if (tick[0] % speed == 0) {
                for (int i = 0; i < reels.size(); i++) {
                    Reward rr = randomReward(pool);
                    if (i == 2 && tick[0] > 48) {
                        rr = result[0];
                    }
                    reels.get(i).getEquipment().setHelmet(rr.preview());
                }
                center.getWorld().playSound(center, Sound.UI_BUTTON_CLICK, 0.45f, 1.35f);
            }
            center.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, center, 2, 0.45, 0.15, 0.45, 0.01);
            if (tick[0] >= 58) {
                taskRef[0].cancel();
                for (ArmorStand as : reels) {
                    as.remove();
                }
                result[0].give(player);
                player.sendMessage(Component.text("Caisse " + def.id + " -> " + result[0].label(), NamedTextColor.GOLD));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
                opening.remove(player.getUniqueId());
            }
        }, 0L, 2L);
    }

    private Reward randomWeighted(List<Reward> pool) {
        int total = pool.stream().mapToInt(Reward::weight).sum();
        int r = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        int acc = 0;
        for (Reward reward : pool) {
            acc += reward.weight();
            if (r < acc) {
                return reward;
            }
        }
        return pool.get(0);
    }

    private Reward randomReward(List<Reward> pool) {
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private List<Reward> rewardPool(String crateId) {
        List<Reward> rewards = new ArrayList<>();
        rewards.add(itemReward("Bandages (moyen) x6", () -> bandageManager.createBandage(6), 14));
        rewards.add(itemReward("Bandages (grand) x4", () -> bandageManager.createConsumable(DpmrConsumable.BANDAGE_LARGE, 4), 10));
        rewards.add(itemReward("Medikit x2", () -> bandageManager.createConsumable(DpmrConsumable.MEDIKIT, 2), 8));
        rewards.add(itemReward("Potion de bouclier (grande) x2", () -> bandageManager.createConsumable(DpmrConsumable.SHIELD_POTION_LARGE, 2), 6));
        rewards.add(pointsReward(20, 16));
        rewards.add(pointsReward(50, 10));
        rewards.add(weaponReward("CARABINE_MK18", 7));
        rewards.add(weaponReward("AK47", 7));
        rewards.add(weaponReward("PULSE", 6));
        rewards.add(weaponReward("FUSIL_POMPE_RL", 6));
        rewards.add(weaponReward("DRAGUNOV_SVD", 4));
        if (crateId.equalsIgnoreCase("legend") || crateId.equalsIgnoreCase("legendaire")) {
            rewards.add(weaponReward("AWP", 3));
            rewards.add(weaponReward("DIVISER_POUR_MIEUX_REGNER", 1));
            rewards.add(pointsReward(180, 3));
        }
        return rewards;
    }

    private Reward itemReward(String label, Supplier<ItemStack> supplier, int weight) {
        return new Reward() {
            @Override
            public ItemStack preview() {
                return supplier.get().clone();
            }
            @Override
            public void give(Player player) {
                player.getInventory().addItem(supplier.get());
            }
            @Override
            public String label() {
                return label;
            }
            @Override
            public int weight() {
                return weight;
            }
        };
    }

    private Reward pointsReward(int amount, int weight) {
        return new Reward() {
            @Override
            public ItemStack preview() {
                ItemStack it = new ItemStack(Material.SUNFLOWER);
                ItemMeta meta = it.getItemMeta();
                meta.displayName(Component.text("Points +" + amount, NamedTextColor.YELLOW));
                it.setItemMeta(meta);
                return it;
            }
            @Override
            public void give(Player player) {
                pointsManager.addPoints(player.getUniqueId(), amount);
                pointsManager.save();
            }
            @Override
            public String label() {
                return amount + " points";
            }
            @Override
            public int weight() {
                return weight;
            }
        };
    }

    private Reward weaponReward(String weaponId, int weight) {
        return new Reward() {
            @Override
            public ItemStack preview() {
                ItemStack w = weaponManager.createWeaponItem(weaponId);
                return w != null ? w : new ItemStack(Material.BARRIER);
            }
            @Override
            public void give(Player player) {
                ItemStack w = weaponManager.createWeaponItem(weaponId);
                if (w != null) {
                    player.getInventory().addItem(w);
                }
            }
            @Override
            public String label() {
                return weaponId;
            }
            @Override
            public int weight() {
                return weight;
            }
        };
    }
}

