package fr.dpmr.game;

import fr.dpmr.armor.ArmorManager;
import fr.dpmr.i18n.GameLocale;
import fr.dpmr.i18n.I18n;
import fr.dpmr.i18n.PlayerLanguageStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager implements Listener {

    private static final String MENU_TITLE = "DPMR Configuration Loot";
    private static final Component MENU_TITLE_COMPONENT = Component.text(MENU_TITLE);

    private final JavaPlugin plugin;
    private final NamespacedKey keyPortableLootChest;
    private final NamespacedKey keyChestBreakerAxe;
    private final WeaponManager weaponManager;
    private final BandageManager bandageManager;
    private final ArmorManager armorManager;
    private final PlayerLanguageStore languageStore;
    private BukkitTask airdropTask;
    private BukkitTask boostTask;
    private BukkitTask chestSpawnTask;

    private final Set<String> zoneChestKeys = new HashSet<>();
    private final Set<String> filledThisChest = new HashSet<>();
    private final Map<String, Long> configuredChestCooldownUntil = new HashMap<>();
    private final Map<String, WeaponRarity> configuredChestRarity = new HashMap<>();
    private final Map<String, ArmorStand> configuredChestHolograms = new HashMap<>();
    private BukkitTask configuredChestHudTask;

    private final Map<String, Inventory> airdropLoot = new HashMap<>();
    private final Set<String> airdropOpened = new HashSet<>();
    private final Map<String, BukkitTask> airdropOpenTasks = new HashMap<>();
    private final Map<String, BlockDisplay> airdropFallingDisplays = new HashMap<>();
    private final Map<String, ArmorStand> airdropHolograms = new HashMap<>();
    private final Map<String, AirdropType> airdropTypes = new HashMap<>();

    private enum AirdropType {
        TACTIQUE("Tactique", NamedTextColor.GOLD, Material.CHEST, Particle.CLOUD, Particle.EXPLOSION, Sound.BLOCK_ANVIL_LAND),
        MEDICAL("Medical", NamedTextColor.GREEN, Material.MOSS_BLOCK, Particle.HAPPY_VILLAGER, Particle.TOTEM_OF_UNDYING, Sound.BLOCK_AMETHYST_BLOCK_RESONATE),
        TECHNO("Techno", NamedTextColor.AQUA, Material.ENDER_CHEST, Particle.ELECTRIC_SPARK, Particle.END_ROD, Sound.BLOCK_BEACON_ACTIVATE);

        final String label;
        final NamedTextColor color;
        final Material fallingMaterial;
        final Particle trailParticle;
        final Particle landParticle;
        final Sound landSound;

        AirdropType(String label, NamedTextColor color, Material fallingMaterial, Particle trailParticle, Particle landParticle, Sound landSound) {
            this.label = label;
            this.color = color;
            this.fallingMaterial = fallingMaterial;
            this.trailParticle = trailParticle;
            this.landParticle = landParticle;
            this.landSound = landSound;
        }
    }

    public LootManager(JavaPlugin plugin, WeaponManager weaponManager, BandageManager bandageManager,
                      ArmorManager armorManager, PlayerLanguageStore languageStore) {
        this.plugin = plugin;
        this.keyPortableLootChest = new NamespacedKey(plugin, "dpmr_portable_loot_chest");
        this.keyChestBreakerAxe = new NamespacedKey(plugin, "dpmr_chest_breaker_axe");
        this.weaponManager = weaponManager;
        this.bandageManager = bandageManager;
        this.armorManager = armorManager;
        this.languageStore = languageStore;
    }

    private static String airdropTypeMessageKey(AirdropType type) {
        return switch (type) {
            case TACTIQUE -> "airdrop.type_tactical";
            case MEDICAL -> "airdrop.type_medical";
            case TECHNO -> "airdrop.type_techno";
        };
    }

    private String englishAirdropTypeLabel(AirdropType type) {
        return I18n.string(GameLocale.EN, airdropTypeMessageKey(type));
    }

    public ItemStack createPortableLootChestItem(int amount) {
        ItemStack item = new ItemStack(Material.CHEST, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Coffre Loot DPMR", NamedTextColor.GOLD, TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text("Pose-le pour enregistrer un coffre DPMR.", NamedTextColor.GRAY),
                Component.text("Le coffre se remplira a l'ouverture.", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(keyPortableLootChest, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createChestBreakerAxeItem() {
        ItemStack item = new ItemStack(Material.STONE_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("AxeChest", NamedTextColor.RED, TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text("Clic droit sur un LootChest pour casser son spawn.", NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(keyChestBreakerAxe, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public int purgeAllLootHolograms() {
        int removed = 0;
        for (ArmorStand holo : configuredChestHolograms.values()) {
            if (removeArmorStandSafe(holo)) {
                removed++;
            }
        }
        configuredChestHolograms.clear();
        for (ArmorStand holo : airdropHolograms.values()) {
            if (removeArmorStandSafe(holo)) {
                removed++;
            }
        }
        airdropHolograms.clear();
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (!stand.isValid() || stand.customName() == null) {
                    continue;
                }
                String plain = PlainTextComponentSerializer.plainText()
                        .serialize(stand.customName())
                        .toLowerCase(Locale.ROOT);
                if (plain.contains("lootchest") || plain.contains("airdrop")) {
                    if (removeArmorStandSafe(stand)) {
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    private static boolean removeArmorStandSafe(ArmorStand stand) {
        if (stand == null || !stand.isValid()) {
            return false;
        }
        stand.remove();
        return true;
    }

    public void startLoops() {
        sanitizeConfiguredWeaponPools();
        refreshConfiguredChestHolograms();
        startConfiguredChestHudTask();
        FileConfiguration cfg = plugin.getConfig();
        long airdropEvery = Math.max(30, cfg.getLong("loot.airdrop-interval-seconds", 300));
        long boostEvery = Math.max(15, cfg.getLong("boosts.spawn-interval-seconds", 120));
        long chestSpawnEvery = Math.max(30, cfg.getLong("loot.chest-spawn-interval-seconds", 180));
        airdropTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnAirdrop, airdropEvery * 20L, airdropEvery * 20L);
        boostTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnBoost, boostEvery * 20L, boostEvery * 20L);
        chestSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::trySpawnZoneChest, chestSpawnEvery * 20L, chestSpawnEvery * 20L);
    }

    private void sanitizeConfiguredWeaponPools() {
        List<String> chestPool = Arrays.stream(WeaponProfile.values())
                .filter(w -> w.rarity() != WeaponRarity.GHOST)
                .map(Enum::name)
                .toList();
        List<String> legendaryPool = Arrays.stream(WeaponProfile.values())
                .filter(w -> w.rarity() == WeaponRarity.LEGENDARY || w.rarity() == WeaponRarity.GHOST)
                .map(Enum::name)
                .toList();
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("loot.chest-weapons", chestPool);
        cfg.set("loot.legendary-weapons", legendaryPool);
        plugin.saveConfig();
    }

    public void stopLoops() {
        if (airdropTask != null) {
            airdropTask.cancel();
            airdropTask = null;
        }
        if (boostTask != null) {
            boostTask.cancel();
            boostTask = null;
        }
        if (chestSpawnTask != null) {
            chestSpawnTask.cancel();
            chestSpawnTask = null;
        }
        for (String key : new HashSet<>(zoneChestKeys)) {
            removeChestBlockAtKey(key);
        }
        zoneChestKeys.clear();
        filledThisChest.clear();
        configuredChestCooldownUntil.clear();
        configuredChestRarity.clear();
        if (configuredChestHudTask != null) {
            configuredChestHudTask.cancel();
            configuredChestHudTask = null;
        }
        for (ArmorStand holo : configuredChestHolograms.values()) {
            if (holo != null && holo.isValid()) {
                holo.remove();
            }
        }
        configuredChestHolograms.clear();
        for (ArmorStand holo : airdropHolograms.values()) {
            if (holo != null && holo.isValid()) {
                holo.remove();
            }
        }
        airdropHolograms.clear();
        for (BlockDisplay disp : airdropFallingDisplays.values()) {
            if (disp != null && disp.isValid()) {
                disp.remove();
            }
        }
        airdropFallingDisplays.clear();
        airdropTypes.clear();
    }

    public void openAdminMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE_COMPONENT);
        inv.setItem(10, named(Material.CHEST, "Ajouter coffre d'armes", NamedTextColor.GREEN));
        inv.setItem(11, named(Material.OAK_SIGN, "Zone: position 1", NamedTextColor.YELLOW));
        inv.setItem(12, named(Material.SPRUCE_SIGN, "Zone: position 2", NamedTextColor.GOLD));
        inv.setItem(13, named(Material.LIME_DYE, "Activer zone + monde", NamedTextColor.GREEN));
        inv.setItem(14, named(Material.CROSSBOW, "Ajouter arme DPMR tenue", NamedTextColor.GOLD));
        inv.setItem(15, named(Material.NETHERITE_SWORD, "Ajouter arme legendaire DPMR", NamedTextColor.LIGHT_PURPLE));
        inv.setItem(16, named(Material.BEACON, "Forcer largage legendaire", NamedTextColor.AQUA));
        player.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!MENU_TITLE_COMPONENT.equals(event.getView().title())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        switch (clicked.getType()) {
            case CHEST -> {
                Block target = player.getTargetBlockExact(6);
                if (target == null || target.getType() != Material.CHEST) {
                    player.sendMessage(Component.text("Regarde un coffre a 6 blocs max.", NamedTextColor.RED));
                    return;
                }
                saveLocation("loot.weapon-chests", target.getLocation());
                player.sendMessage(Component.text("Coffre fixe: disparait apres pillage puis respawn (voir config).", NamedTextColor.GREEN));
            }
            case OAK_SIGN -> {
                saveZoneCorner(player, "loot.chest-spawn-zone.corner-a");
                player.sendMessage(Component.text("Zone coffre: position 1 enregistree.", NamedTextColor.YELLOW));
            }
            case SPRUCE_SIGN -> {
                saveZoneCorner(player, "loot.chest-spawn-zone.corner-b");
                player.sendMessage(Component.text("Zone coffre: position 2 enregistree.", NamedTextColor.GOLD));
            }
            case LIME_DYE -> {
                plugin.getConfig().set("loot.chest-spawn-zone.enabled", true);
                plugin.getConfig().set("loot.chest-spawn-zone.world", player.getWorld().getName());
                plugin.saveConfig();
                player.sendMessage(Component.text("Zone coffre activee sur le monde actuel. Mets 2 coins avec les panneaux du menu.", NamedTextColor.GREEN));
            }
            case CROSSBOW -> addHeldWeapon(player, "loot.chest-weapons");
            case NETHERITE_SWORD -> addHeldWeapon(player, "loot.legendary-weapons");
            case BEACON -> {
                spawnAirdrop();
                player.sendMessage(Component.text("Largage force.", NamedTextColor.AQUA));
            }
            default -> { }
        }
    }

    @EventHandler
    public void onPortableLootChestPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.CHEST) {
            return;
        }
        ItemStack inHand = event.getItemInHand();
        if (inHand == null || !inHand.hasItemMeta()) {
            return;
        }
        Byte tag = inHand.getItemMeta().getPersistentDataContainer().get(keyPortableLootChest, PersistentDataType.BYTE);
        if (tag == null || tag != (byte) 1) {
            return;
        }
        saveLocation("loot.weapon-chests", event.getBlockPlaced().getLocation());
        refreshConfiguredChestHolograms();
        Player player = event.getPlayer();
        player.sendMessage(Component.text("Coffre DPMR place et enregistre.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.35f);
    }

    private void saveZoneCorner(Player player, String path) {
        Location loc = player.getLocation().getBlock().getLocation();
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getBlockX());
        plugin.getConfig().set(path + ".y", loc.getBlockY());
        plugin.getConfig().set(path + ".z", loc.getBlockZ());
        plugin.saveConfig();
    }

    @EventHandler
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }
        if (isChestBreakerAxe(event.getItem())) {
            if (breakConfiguredChestSpawn(block, event.getPlayer())) {
                event.setCancelled(true);
            }
            return;
        }
        Location loc = block.getLocation();
        if (!isDpmrChest(loc)) {
            return;
        }
        String configuredKey = configuredChestKeyFromStructure(block);
        if (configuredKey != null) {
            long left = configuredChestCooldownUntil.getOrDefault(configuredKey, 0L) - System.currentTimeMillis();
            if (left > 0L) {
                event.setCancelled(true);
                long sec = Math.max(1L, (long) Math.ceil(left / 1000.0));
                event.getPlayer().sendActionBar(Component.text("LootChest recharge dans " + sec + "s", NamedTextColor.YELLOW));
                return;
            }
        }
        Set<String> structureKeys = connectedChestKeys(block);
        if (structureKeys.stream().noneMatch(filledThisChest::contains)) {
            if (block.getState() instanceof Chest chest) {
                WeaponRarity forcedRarity = configuredKey != null ? configuredChestRarity.get(configuredKey) : null;
                fillLootChest(chest.getInventory(), forcedRarity);
                filledThisChest.addAll(structureKeys);
            }
        }
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        HumanEntity human = event.getPlayer();
        if (!(human instanceof Player)) {
            return;
        }
        Inventory inv = event.getInventory();
        Location holderLoc = inv.getLocation();
        if (holderLoc == null) {
            return;
        }
        Block block = holderLoc.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }
        if (!isDpmrChest(block.getLocation())) {
            return;
        }
        Set<String> structureKeys = connectedChestKeys(block);
        List<String> cfgChests = plugin.getConfig().getStringList("loot.weapon-chests");
        boolean touchesConfigured = structureKeys.stream().anyMatch(cfgChests::contains);

        for (ItemStack stack : inv.getContents()) {
            if (stack != null && !stack.getType().isAir()) {
                holderLoc.getWorld().dropItemNaturally(holderLoc.clone().add(0.5, 0.6, 0.5), stack.clone());
            }
        }
        inv.clear();
        for (String k : structureKeys) {
            filledThisChest.remove(k);
        }
        if (touchesConfigured) {
            String canonical = structureKeys.stream()
                    .filter(cfgChests::contains)
                    .min(String::compareTo)
                    .orElseGet(() -> locKey(holderLoc.getBlock().getLocation()));
            scheduleConfiguredChestRespawn(canonical);
            WeaponRarity rarity = configuredChestRarity.get(canonical);
            if (event.getPlayer() instanceof Player pl) {
                pl.sendActionBar(Component.text("LootChest " , NamedTextColor.GRAY)
                        .append(rarityDisplay(rarity))
                        .append(Component.text(" en recharge.", NamedTextColor.GRAY)));
            }
        } else {
            removeChestBlocks(block);
            for (String k : structureKeys) {
                zoneChestKeys.remove(k);
            }
        }

        HumanEntity p = event.getPlayer();
        if (p instanceof Player pl) {
            pl.playSound(pl.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
            if (!touchesConfigured) {
                pl.sendActionBar(Component.text("Coffre ramasse.", NamedTextColor.GRAY));
            }
        }
    }

    @EventHandler
    public void onBoostPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() != Material.BEACON || item.getItemMeta() == null || item.getItemMeta().displayName() == null) {
            return;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName()).toUpperCase(Locale.ROOT);
        if (plain.contains("INVINCIBILITY")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 10, 4));
        } else if (plain.contains("RAPID_FIRE")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 15, 3));
        } else if (plain.contains("SPEED")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 15, 2));
        } else {
            return;
        }
        player.sendMessage(Component.text("Boost active.", NamedTextColor.GREEN));
    }

    public void spawnAirdrop() {
        Location rawTarget = pickAirdropTarget();
        Location target = clampAirdropToGround(rawTarget);
        if (target == null || target.getWorld() == null) {
            return;
        }
        World world = target.getWorld();
        int spawnH = Math.max(10, plugin.getConfig().getInt("loot.airdrop.spawn-height", 28));
        Location spawn = target.clone().add(0.5, spawnH, 0.5);

        String key = locKey(target);
        if (airdropFallingDisplays.containsKey(key) || airdropLoot.containsKey(key)) {
            return;
        }
        AirdropType type = randomAirdropType();
        airdropTypes.put(key, type);

        world.spawnParticle(Particle.FIREWORK, spawn, 45, 1.2, 1.2, 1.2, 0.02);
        world.playSound(spawn, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.9f, 1.1f);
        for (Player p : Bukkit.getOnlinePlayers()) {
            String tlabel = I18n.string(languageStore.get(p), airdropTypeMessageKey(type));
            p.sendMessage(I18n.component(languageStore.get(p), type.color, "airdrop.approaching", tlabel));
        }

        BlockDisplay disp = world.spawn(spawn, BlockDisplay.class);
        disp.setBlock(Bukkit.createBlockData(type.fallingMaterial));
        disp.setTransformation(new Transformation(
                new org.joml.Vector3f(0f, 0f, 0f),
                new org.joml.Quaternionf(),
                new org.joml.Vector3f(1f, 1f, 1f),
                new org.joml.Quaternionf()
        ));
        airdropFallingDisplays.put(key, disp);
        ArmorStand holo = spawnAirdropHologram(spawn.clone().add(0, 1.25, 0), type.color,
                "Airdrop " + englishAirdropTypeLabel(type) + " incoming");
        airdropHolograms.put(key, holo);

        double speed = Math.max(0.01, plugin.getConfig().getDouble("loot.airdrop.fall-speed", 0.06));
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!disp.isValid()) {
                airdropFallingDisplays.remove(key);
                removeAirdropHologram(key);
                task.cancel();
                return;
            }
            Location cur = disp.getLocation();
            Location next = cur.clone().add(0, -speed, 0);
            disp.teleport(next);
            ArmorStand h = airdropHolograms.get(key);
            if (h != null && h.isValid()) {
                h.teleport(next.clone().add(0, 1.25, 0));
            }
            world.spawnParticle(type.trailParticle, next.clone().add(0, 0.4, 0), 2, 0.05, 0.05, 0.05, 0.001);

            if (next.getY() <= target.getY() + 0.5) {
                disp.remove();
                airdropFallingDisplays.remove(key);
                placeAirdropChest(target);
                updateAirdropHologramLanded(key, target.clone().add(0.5, 1.6, 0.5));
                world.playSound(target, type.landSound, 0.9f, 1.2f);
                world.spawnParticle(type.landParticle, target.clone().add(0.5, 0.6, 0.5), 12, 0.25, 0.25, 0.25, 0.01);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String tlabel = I18n.string(languageStore.get(p), airdropTypeMessageKey(type));
                    p.sendMessage(I18n.component(languageStore.get(p), type.color, "airdrop.landed",
                            tlabel, target.getBlockX(), target.getBlockY(), target.getBlockZ()));
                }
                task.cancel();
            }
        }, 1L, 1L);
    }

    private AirdropType randomAirdropType() {
        AirdropType[] values = AirdropType.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private Location clampAirdropToGround(Location target) {
        if (target == null || target.getWorld() == null) {
            return null;
        }
        World world = target.getWorld();
        int x = target.getBlockX();
        int z = target.getBlockZ();
        int groundY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        Location out = new Location(world, x, groundY + 1, z);
        Material under = out.clone().subtract(0, 1, 0).getBlock().getType();
        if (!under.isSolid()) {
            return null;
        }
        return out;
    }

    public void setAirdropLocation(Player player) {
        Location loc = player.getLocation().getBlock().getLocation();
        plugin.getConfig().set("loot.airdrop.fixed-enabled", true);
        plugin.getConfig().set("loot.airdrop.fixed-location.world", loc.getWorld().getName());
        plugin.getConfig().set("loot.airdrop.fixed-location.x", loc.getBlockX());
        plugin.getConfig().set("loot.airdrop.fixed-location.y", loc.getBlockY());
        plugin.getConfig().set("loot.airdrop.fixed-location.z", loc.getBlockZ());
        plugin.saveConfig();
        player.sendMessage(Component.text("Airdrop fixe: " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ(), NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.6f);
    }

    private Location pickAirdropTarget() {
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.getBoolean("loot.airdrop.fixed-enabled", false)) {
            String w = cfg.getString("loot.airdrop.fixed-location.world", "world");
            World world = Bukkit.getWorld(w);
            if (world == null) {
                return null;
            }
            int x = cfg.getInt("loot.airdrop.fixed-location.x", 0);
            int y = cfg.getInt("loot.airdrop.fixed-location.y", 80);
            int z = cfg.getInt("loot.airdrop.fixed-location.z", 0);
            return new Location(world, x, y, z);
        }
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            return null;
        }
        Player ref = online.get(ThreadLocalRandom.current().nextInt(online.size()));
        Location base = ref.getLocation().getBlock().getLocation();
        return base.add(ThreadLocalRandom.current().nextInt(-25, 26), 0, ThreadLocalRandom.current().nextInt(-25, 26));
    }

    private void placeAirdropChest(Location targetBlock) {
        Location loc = targetBlock.getBlock().getLocation();
        Block b = loc.getBlock();
        if (!b.getType().isAir() && b.getType().isSolid()) {
            return;
        }
        b.setType(Material.CHEST);
        String key = locKey(loc);
        AirdropType dropType = airdropTypes.getOrDefault(key, AirdropType.TACTIQUE);
        airdropLoot.put(key, generateAirdropLoot(dropType));
        airdropOpened.remove(key);
    }

    private Inventory generateAirdropLoot(AirdropType type) {
        if (type == AirdropType.MEDICAL) {
            return generateMedicalAirdropLoot();
        }
        return generateWeaponAirdropLoot();
    }

    private Inventory generateWeaponAirdropLoot() {
        FileConfiguration cfg = plugin.getConfig();
        int min = Math.max(1, cfg.getInt("loot.airdrop.rolls-min", 1));
        int max = Math.max(min, cfg.getInt("loot.airdrop.rolls-max", 2));
        int rolls = Math.max(1, Math.min(2, ThreadLocalRandom.current().nextInt(min, max + 1)));
        double legChance = Math.max(0, Math.min(0.2, cfg.getDouble("loot.airdrop.legendary-chance", 0.55) * 0.35));
        List<String> leg = cfg.getStringList("loot.legendary-weapons");

        List<String> epic = Arrays.stream(WeaponProfile.values())
                .filter(w -> w.rarity() == WeaponRarity.EPIC)
                .map(Enum::name)
                .toList();

        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Airdrop", NamedTextColor.GOLD, TextDecoration.BOLD));
        inv.clear();
        for (int i = 0; i < rolls; i++) {
            boolean takeLeg = !leg.isEmpty() && ThreadLocalRandom.current().nextDouble() < legChance;
            String id = takeLeg
                    ? leg.get(ThreadLocalRandom.current().nextInt(leg.size()))
                    : (epic.isEmpty() ? null : epic.get(ThreadLocalRandom.current().nextInt(epic.size())));
            if (id == null) {
                continue;
            }
            ItemStack w = weaponManager.createWeaponItem(id);
            if (w != null) {
                inv.addItem(w);
            }
        }
        return inv;
    }

    private Inventory generateMedicalAirdropLoot() {
        FileConfiguration cfg = plugin.getConfig();
        String base = "loot.airdrop.medical.";
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Airdrop", NamedTextColor.GOLD, TextDecoration.BOLD));
        inv.clear();

        int bandStacksMin = Math.max(0, cfg.getInt(base + "bandage-stacks-min", 3));
        int bandStacksMax = Math.max(bandStacksMin, cfg.getInt(base + "bandage-stacks-max", 5));
        int bandPerMin = Math.max(1, cfg.getInt(base + "bandages-per-stack-min", 2));
        int bandPerMax = Math.max(bandPerMin, cfg.getInt(base + "bandages-per-stack-max", 6));
        int bandStacks = ThreadLocalRandom.current().nextInt(bandStacksMin, bandStacksMax + 1);
        for (int s = 0; s < bandStacks; s++) {
            int n = ThreadLocalRandom.current().nextInt(bandPerMin, bandPerMax + 1);
            inv.addItem(bandageManager.createConsumable(bandageManager.rollLootHealConsumable(), n));
        }

        int shieldMin = Math.max(0, cfg.getInt(base + "shield-potion-stacks-min", 1));
        int shieldMax = Math.max(shieldMin, cfg.getInt(base + "shield-potion-stacks-max", 2));
        int shieldStacks = ThreadLocalRandom.current().nextInt(shieldMin, shieldMax + 1);
        int shPerMin = Math.max(1, cfg.getInt(base + "shield-potion-per-stack-min", 1));
        int shPerMax = Math.max(shPerMin, cfg.getInt(base + "shield-potion-per-stack-max", 2));
        for (int s = 0; s < shieldStacks; s++) {
            int n = ThreadLocalRandom.current().nextInt(shPerMin, shPerMax + 1);
            inv.addItem(bandageManager.createConsumable(bandageManager.rollLootShieldPotion(), n));
        }

        int mediMin = Math.max(0, cfg.getInt(base + "medikit-stacks-min", 1));
        int mediMax = Math.max(mediMin, cfg.getInt(base + "medikit-stacks-max", 2));
        int mediStacks = ThreadLocalRandom.current().nextInt(mediMin, mediMax + 1);
        int mediPerMin = Math.max(1, cfg.getInt(base + "medikit-per-stack-min", 1));
        int mediPerMax = Math.max(mediPerMin, cfg.getInt(base + "medikit-per-stack-max", 2));
        for (int s = 0; s < mediStacks; s++) {
            int n = ThreadLocalRandom.current().nextInt(mediPerMin, mediPerMax + 1);
            inv.addItem(bandageManager.createConsumable(DpmrConsumable.MEDIKIT, n));
        }

        if (cfg.getBoolean(base + "include-lance-soin", true)) {
            ItemStack lance = weaponManager.createWeaponItem(WeaponProfile.LANCE_SOIN.name());
            if (lance != null) {
                inv.addItem(lance);
            }
        }
        if (cfg.getBoolean(base + "include-serum-soin", true)) {
            ItemStack serum = weaponManager.createWeaponItem(WeaponProfile.SERUM_SOIN.name());
            if (serum != null) {
                inv.addItem(serum);
            }
        }
        return inv;
    }

    private void startAirdropOpen(Player player, Location chestLoc) {
        String key = locKey(chestLoc);
        if (airdropOpened.contains(key)) {
            I18n.actionBar(player, NamedTextColor.GRAY, "airdrop.already_open");
            return;
        }
        if (!airdropLoot.containsKey(key)) {
            return;
        }
        String sessionKey = key + ":" + player.getUniqueId();
        if (airdropOpenTasks.containsKey(sessionKey)) {
            return;
        }
        int sec = Math.max(1, plugin.getConfig().getInt("loot.airdrop.open-seconds", 5));
        int totalTicks = sec * 20;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.25f);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int left = totalTicks;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    BukkitTask t = airdropOpenTasks.remove(sessionKey);
                    if (t != null) t.cancel();
                    return;
                }
                left--;
                int pct = (int) Math.round(100.0 * (totalTicks - left) / totalTicks);
                I18n.actionBar(player, NamedTextColor.GOLD, "airdrop.opening", pct);
                if (left % 10 == 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.4f);
                }
                if (left <= 0) {
                    airdropOpened.add(key);
                    Inventory loot = airdropLoot.get(key);
                    if (loot != null) {
                        player.openInventory(loot);
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.9f, 1.15f);
                    }
                    BukkitTask t = airdropOpenTasks.remove(sessionKey);
                    if (t != null) t.cancel();
                }
            }
        }, 1L, 1L);
        airdropOpenTasks.put(sessionKey, task);
    }

    @EventHandler
    public void onAirdropInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.CHEST) {
            return;
        }
        Location loc = event.getClickedBlock().getLocation();
        String key = locKey(loc);
        if (!airdropLoot.containsKey(key)) {
            return;
        }
        event.setCancelled(true);
        startAirdropOpen(event.getPlayer(), loc);
    }

    @EventHandler
    public void onAirdropLootClose(InventoryCloseEvent event) {
        InventoryView view = event.getView();
        if (view.title() == null) {
            return;
        }
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(view.title()).equals("Airdrop")) {
            return;
        }
        Inventory inv = view.getTopInventory();
        // Si plus d'items, on nettoie le coffre le plus proche dans un petit rayon
        boolean empty = true;
        for (ItemStack s : inv.getContents()) {
            if (s != null && !s.getType().isAir()) {
                empty = false;
                break;
            }
        }
        if (!empty) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Location pLoc = player.getLocation();
        String best = null;
        double bestD = 6 * 6;
        for (String k : airdropLoot.keySet()) {
            Location c = parseLocKey(k);
            if (c == null || c.getWorld() == null || !c.getWorld().equals(pLoc.getWorld())) {
                continue;
            }
            double d = c.distanceSquared(pLoc);
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        if (best != null) {
            Location chest = parseLocKey(best);
            if (chest != null && chest.getBlock().getType() == Material.CHEST) {
                chest.getBlock().setType(Material.AIR);
                chest.getWorld().playSound(chest, Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.2f);
                chest.getWorld().spawnParticle(Particle.END_ROD, chest.clone().add(0.5, 0.6, 0.5), 18, 0.35, 0.35, 0.35, 0.02);
            }
            airdropLoot.remove(best);
            airdropOpened.remove(best);
            removeAirdropHologram(best);
        }
    }

    private ArmorStand spawnAirdropHologram(Location at, NamedTextColor color, String text) {
        if (at.getWorld() == null) {
            return null;
        }
        ArmorStand as = at.getWorld().spawn(at, ArmorStand.class);
        as.setInvisible(true);
        as.setMarker(true);
        as.setGravity(false);
        as.setCustomNameVisible(true);
        as.customName(Component.text(text, color, TextDecoration.BOLD));
        return as;
    }

    private void updateAirdropHologramLanded(String key, Location at) {
        ArmorStand holo = airdropHolograms.get(key);
        if (holo == null || !holo.isValid()) {
            return;
        }
        AirdropType type = airdropTypes.getOrDefault(key, AirdropType.TACTIQUE);
        holo.teleport(at);
        holo.customName(Component.text("Airdrop " + type.label, type.color, TextDecoration.BOLD)
                .append(Component.text(" • Clic droit", NamedTextColor.YELLOW)));
    }

    private void removeAirdropHologram(String key) {
        ArmorStand holo = airdropHolograms.remove(key);
        if (holo != null && holo.isValid()) {
            holo.remove();
        }
        airdropTypes.remove(key);
    }

    public void forceSpawnZoneChest() {
        trySpawnZoneChest();
    }

    private void trySpawnZoneChest() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("loot.chest-spawn-zone.enabled", false)) {
            return;
        }
        int maxActive = Math.max(1, cfg.getInt("loot.chest-spawn-max-active", 5));
        if (zoneChestKeys.size() >= maxActive) {
            return;
        }
        Location surface = randomSurfaceInZone();
        if (surface == null) {
            return;
        }
        Block b = surface.getBlock();
        b.setType(Material.CHEST);
        String key = locKey(surface);
        zoneChestKeys.add(key);
        Bukkit.broadcast(Component.text("[DPMR] Un coffre de butin est apparu !", NamedTextColor.GREEN));
        surface.getWorld().playSound(surface, Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
    }

    private Location randomSurfaceInZone() {
        FileConfiguration cfg = plugin.getConfig();
        String worldName = cfg.getString("loot.chest-spawn-zone.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        int ax = cfg.getInt("loot.chest-spawn-zone.corner-a.x", 0);
        int ay = cfg.getInt("loot.chest-spawn-zone.corner-a.y", 60);
        int az = cfg.getInt("loot.chest-spawn-zone.corner-a.z", 0);
        int bx = cfg.getInt("loot.chest-spawn-zone.corner-b.x", 16);
        int by = cfg.getInt("loot.chest-spawn-zone.corner-b.y", 80);
        int bz = cfg.getInt("loot.chest-spawn-zone.corner-b.z", 16);
        int minX = Math.min(ax, bx);
        int maxX = Math.max(ax, bx);
        int minZ = Math.min(az, bz);
        int maxZ = Math.max(az, bz);
        int minY = Math.min(ay, by);
        int maxY = Math.max(ay, by);
        if (maxX - minX < 1 || maxZ - minZ < 1) {
            return null;
        }
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
            int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (y < minY) {
                y = minY;
            }
            if (y > maxY) {
                continue;
            }
            Location loc = new Location(world, x, y + 1, z);
            if (loc.getBlock().getType().isAir() && loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                return loc;
            }
        }
        return null;
    }

    private void spawnBoost() {
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            return;
        }
        Player ref = online.get(ThreadLocalRandom.current().nextInt(online.size()));
        Location dropLoc = ref.getLocation().clone().add(
                ThreadLocalRandom.current().nextDouble(-20, 20),
                2.5,
                ThreadLocalRandom.current().nextDouble(-20, 20)
        );
        List<String> boosts = plugin.getConfig().getStringList("boosts.available");
        if (boosts.isEmpty()) {
            return;
        }
        String boost = boosts.get(ThreadLocalRandom.current().nextInt(boosts.size())).toUpperCase(Locale.ROOT);
        ItemStack stack = named(Material.BEACON, "BOOST_" + boost, NamedTextColor.AQUA);
        Item item = dropLoc.getWorld().dropItem(dropLoc, stack);
        item.setGlowing(true);
        Bukkit.broadcast(Component.text("[DPMR] Un boost est apparu !", NamedTextColor.AQUA));
    }

    private void fillLootChest(Inventory inventory, WeaponRarity forcedRarity) {
        inventory.clear();
        FileConfiguration cfg = plugin.getConfig();
        int weaponMin = cfg.getInt("loot.chest-weapon-min", -1);
        int weaponMax = cfg.getInt("loot.chest-weapon-max", -1);
        if (weaponMin < 0 || weaponMax < 0) {
            weaponMin = Math.min(1, Math.max(0, cfg.getInt("loot.chest-rolls-min", 0)));
            weaponMax = Math.min(1, Math.max(weaponMin, cfg.getInt("loot.chest-rolls-max", 1)));
        } else {
            weaponMin = Math.max(0, Math.min(1, weaponMin));
            weaponMax = Math.max(weaponMin, Math.min(1, weaponMax));
        }
        int weaponRolls = weaponMin == weaponMax ? weaponMin : ThreadLocalRandom.current().nextInt(weaponMin, weaponMax + 1);
        Set<WeaponProfile> rolledThisChest = new HashSet<>();
        for (int i = 0; i < weaponRolls; i++) {
            WeaponProfile rolled = rollWeaponForChest(cfg, rolledThisChest, forcedRarity);
            if (rolled == null) {
                continue;
            }
            rolledThisChest.add(rolled);
            ItemStack weapon = weaponManager.createWeaponItem(rolled.name());
            placeLootItem(inventory, weapon, true);
        }
        int armorMin = Math.max(0, cfg.getInt("loot.chest-armor-min", 0));
        int armorMax = Math.max(armorMin, cfg.getInt("loot.chest-armor-max", 1));
        armorMax = Math.min(4, armorMax);
        int armorPieces = armorMin == armorMax ? armorMin : ThreadLocalRandom.current().nextInt(armorMin, armorMax + 1);
        for (int a = 0; a < armorPieces; a++) {
            ItemStack piece = armorManager.createRandomArmorPiece();
            placeLootItem(inventory, piece, true);
        }
        int bandStacksMin = Math.max(0, cfg.getInt("loot.bandage-stacks-min", 0));
        int bandStacksMax = Math.max(bandStacksMin, cfg.getInt("loot.bandage-stacks-max", 4));
        int stacks = ThreadLocalRandom.current().nextInt(bandStacksMin, bandStacksMax + 1);
        int perStackMin = Math.max(1, cfg.getInt("loot.bandages-per-stack-min", 1));
        int perStackMax = Math.max(perStackMin, cfg.getInt("loot.bandages-per-stack-max", 3));
        for (int s = 0; s < stacks; s++) {
            int bandCount = ThreadLocalRandom.current().nextInt(perStackMin, perStackMax + 1);
            DpmrConsumable healType = bandageManager.rollLootHealConsumable();
            placeLootItem(inventory, bandageManager.createConsumable(healType, bandCount), false);
        }
        int shieldStacksMin = Math.max(0, cfg.getInt("loot.shield-potion-stacks-min", 0));
        int shieldStacksMax = Math.max(shieldStacksMin, cfg.getInt("loot.shield-potion-stacks-max", 1));
        int shieldStacks = ThreadLocalRandom.current().nextInt(shieldStacksMin, shieldStacksMax + 1);
        int shieldPerMin = Math.max(1, cfg.getInt("loot.shield-potion-per-stack-min", 1));
        int shieldPerMax = Math.max(shieldPerMin, cfg.getInt("loot.shield-potion-per-stack-max", 1));
        for (int s = 0; s < shieldStacks; s++) {
            int n = ThreadLocalRandom.current().nextInt(shieldPerMin, shieldPerMax + 1);
            DpmrConsumable shieldType = bandageManager.rollLootShieldPotion();
            placeLootItem(inventory, bandageManager.createConsumable(shieldType, n), false);
        }
    }

    /**
     * Coffre événement hélicoptère : plusieurs armes épique/légendaire, armures, soins.
     */
    public void fillHelicopterSupplyChest(Inventory inventory) {
        inventory.clear();
        FileConfiguration cfg = plugin.getConfig();
        String base = "npc-war-world.helicopter.chest.";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int wMin = Math.max(1, cfg.getInt(base + "weapon-rolls-min", 2));
        int wMax = Math.max(wMin, cfg.getInt(base + "weapon-rolls-max", 4));
        double legChance = Math.min(1.0, Math.max(0.0, cfg.getDouble(base + "legendary-chance-per-weapon", 0.38)));
        int weaponRolls = wMin == wMax ? wMin : rnd.nextInt(wMin, wMax + 1);
        Set<WeaponProfile> rolledThisChest = new HashSet<>();
        for (int i = 0; i < weaponRolls; i++) {
            WeaponRarity tier = rnd.nextDouble() < legChance ? WeaponRarity.LEGENDARY : WeaponRarity.EPIC;
            WeaponProfile rolled = rollWeaponForChest(cfg, rolledThisChest, tier);
            if (rolled == null) {
                continue;
            }
            rolledThisChest.add(rolled);
            ItemStack weapon = weaponManager.createWeaponItem(rolled.name());
            placeLootItem(inventory, weapon, true);
        }
        int armorMin = Math.max(0, cfg.getInt(base + "armor-pieces-min", 1));
        int armorMax = Math.max(armorMin, cfg.getInt(base + "armor-pieces-max", 2));
        armorMax = Math.min(4, armorMax);
        int armorPieces = armorMin == armorMax ? armorMin : rnd.nextInt(armorMin, armorMax + 1);
        for (int a = 0; a < armorPieces; a++) {
            placeLootItem(inventory, armorManager.createRandomArmorPiece(), true);
        }
        int bandStacksMin = Math.max(0, cfg.getInt(base + "bandage-stacks-min", 3));
        int bandStacksMax = Math.max(bandStacksMin, cfg.getInt(base + "bandage-stacks-max", 6));
        int stacks = rnd.nextInt(bandStacksMin, bandStacksMax + 1);
        int perStackMin = Math.max(1, cfg.getInt(base + "bandages-per-stack-min", 2));
        int perStackMax = Math.max(perStackMin, cfg.getInt(base + "bandages-per-stack-max", 5));
        for (int s = 0; s < stacks; s++) {
            int bandCount = rnd.nextInt(perStackMin, perStackMax + 1);
            DpmrConsumable healType = bandageManager.rollLootHealConsumable();
            placeLootItem(inventory, bandageManager.createConsumable(healType, bandCount), false);
        }
        int shieldStacksMin = Math.max(0, cfg.getInt(base + "shield-potion-stacks-min", 1));
        int shieldStacksMax = Math.max(shieldStacksMin, cfg.getInt(base + "shield-potion-stacks-max", 2));
        int shieldStacks = rnd.nextInt(shieldStacksMin, shieldStacksMax + 1);
        int shieldPerMin = Math.max(1, cfg.getInt(base + "shield-potion-per-stack-min", 1));
        int shieldPerMax = Math.max(shieldPerMin, cfg.getInt(base + "shield-potion-per-stack-max", 2));
        for (int s = 0; s < shieldStacks; s++) {
            int n = rnd.nextInt(shieldPerMin, shieldPerMax + 1);
            DpmrConsumable shieldType = bandageManager.rollLootShieldPotion();
            placeLootItem(inventory, bandageManager.createConsumable(shieldType, n), false);
        }
    }

    /** Armes / armures au centre du coffre en priorité, soins autour. */
    private static void placeLootItem(Inventory inventory, ItemStack stack, boolean preferCenter) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        int slot = preferCenter ? firstPreferredEmptySlot(inventory) : firstEmptySlot(inventory);
        if (slot < 0) {
            slot = firstEmptySlot(inventory);
        }
        if (slot >= 0) {
            inventory.setItem(slot, stack);
        }
    }

    private static int firstPreferredEmptySlot(Inventory inventory) {
        int size = inventory.getSize();
        int center = size / 2;
        if (slotEmpty(inventory, center)) {
            return center;
        }
        for (int radius = 1; radius < size; radius++) {
            for (int sign : new int[]{-1, 1}) {
                int s = center + sign * radius;
                if (s >= 0 && s < size && slotEmpty(inventory, s)) {
                    return s;
                }
            }
        }
        return -1;
    }

    private static boolean slotEmpty(Inventory inventory, int slot) {
        ItemStack it = inventory.getItem(slot);
        return it == null || it.getType().isAir();
    }

    private static int firstEmptySlot(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                return i;
            }
        }
        return -1;
    }

    private boolean isDpmrChest(Location location) {
        Block b = location.getBlock();
        if (b.getType() != Material.CHEST) {
            return false;
        }
        for (String k : connectedChestKeys(b)) {
            if (zoneChestKeys.contains(k)) {
                return true;
            }
            for (String raw : plugin.getConfig().getStringList("loot.weapon-chests")) {
                if (raw.equals(k)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> connectedChestKeys(Block start) {
        Set<String> keys = new HashSet<>();
        if (start.getType() != Material.CHEST) {
            return keys;
        }
        keys.add(locKey(start.getLocation()));
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block n = start.getRelative(face);
            if (n.getType() == Material.CHEST) {
                keys.add(locKey(n.getLocation()));
            }
        }
        return keys;
    }

    private static String locKey(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    private void scheduleConfiguredChestRespawn(String canonicalKey) {
        long sec = Math.max(5L, plugin.getConfig().getLong("loot.configured-chest-respawn-seconds", 120));
        configuredChestCooldownUntil.put(canonicalKey, System.currentTimeMillis() + sec * 1000L);
        configuredChestRarity.put(canonicalKey, rollConfiguredChestRarity(plugin.getConfig()));
        updateConfiguredChestHologram(canonicalKey);
    }

    private void startConfiguredChestHudTask() {
        if (configuredChestHudTask != null) {
            configuredChestHudTask.cancel();
        }
        configuredChestHudTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickConfiguredChestHud, 20L, 20L);
    }

    private void tickConfiguredChestHud() {
        long now = System.currentTimeMillis();
        List<String> done = new ArrayList<>();
        for (Map.Entry<String, Long> e : configuredChestCooldownUntil.entrySet()) {
            if (e.getValue() <= now) {
                done.add(e.getKey());
            }
        }
        for (String key : done) {
            configuredChestCooldownUntil.remove(key);
            Location loc = parseLocKey(key);
            if (loc != null && loc.getWorld() != null && loc.getBlock().getType() == Material.CHEST) {
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.75f, 1.35f);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0.5, 1.0, 0.5), 8, 0.25, 0.2, 0.25, 0.01);
            }
        }
        refreshConfiguredChestHolograms();
        tickEpicConfiguredChestParticles();
    }

    /** Particules autour des LootChest configures dont la rarete affichee est Epique. */
    private void tickEpicConfiguredChestParticles() {
        for (String key : plugin.getConfig().getStringList("loot.weapon-chests")) {
            if (configuredChestRarity.get(key) != WeaponRarity.EPIC) {
                continue;
            }
            Location loc = parseLocKey(key);
            if (loc == null || loc.getWorld() == null || loc.getBlock().getType() != Material.CHEST) {
                continue;
            }
            Location center = loc.clone().add(0.5, 1.15, 0.5);
            World w = loc.getWorld();
            w.spawnParticle(Particle.ENCHANT, center, 10, 0.4, 0.25, 0.4, 0.02);
            w.spawnParticle(Particle.END_ROD, center, 3, 0.12, 0.08, 0.12, 0.01);
        }
    }

    private boolean isChestBreakerAxe(ItemStack item) {
        if (item == null || item.getType() != Material.STONE_AXE || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(keyChestBreakerAxe, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private boolean breakConfiguredChestSpawn(Block block, Player player) {
        String key = configuredChestKeyFromStructure(block);
        if (key == null) {
            player.sendActionBar(Component.text("Ce coffre n'est pas un LootChest configure.", NamedTextColor.GRAY));
            return false;
        }
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList("loot.weapon-chests"));
        if (!list.remove(key)) {
            player.sendActionBar(Component.text("Spawn deja retire.", NamedTextColor.GRAY));
            return false;
        }
        plugin.getConfig().set("loot.weapon-chests", list);
        plugin.saveConfig();
        configuredChestCooldownUntil.remove(key);
        configuredChestRarity.remove(key);
        ArmorStand holo = configuredChestHolograms.remove(key);
        if (holo != null && holo.isValid()) {
            holo.remove();
        }
        removeChestBlocks(block);
        player.sendMessage(Component.text("Spawn LootChest retire: " + key, NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.9f, 0.75f);
        return true;
    }

    private String configuredChestKeyFromStructure(Block block) {
        Set<String> keys = connectedChestKeys(block);
        List<String> cfg = plugin.getConfig().getStringList("loot.weapon-chests");
        return keys.stream().filter(cfg::contains).min(String::compareTo).orElse(null);
    }

    private void refreshConfiguredChestHolograms() {
        List<String> configured = plugin.getConfig().getStringList("loot.weapon-chests");
        Set<String> wanted = new HashSet<>(configured);
        for (String existing : new HashSet<>(configuredChestHolograms.keySet())) {
            if (!wanted.contains(existing)) {
                ArmorStand holo = configuredChestHolograms.remove(existing);
                if (holo != null && holo.isValid()) {
                    holo.remove();
                }
            }
        }
        for (String key : configured) {
            updateConfiguredChestHologram(key);
        }
    }

    private void updateConfiguredChestHologram(String key) {
        Location loc = parseLocKey(key);
        if (loc == null || loc.getWorld() == null || loc.getBlock().getType() != Material.CHEST) {
            ArmorStand old = configuredChestHolograms.remove(key);
            if (old != null && old.isValid()) {
                old.remove();
            }
            return;
        }
        Location holoLoc = loc.clone().add(0.5, 1.35, 0.5);
        ArmorStand holo = configuredChestHolograms.get(key);
        if (holo == null || !holo.isValid()) {
            holo = spawnAirdropHologram(holoLoc, NamedTextColor.GOLD, "LootChest");
            configuredChestHolograms.put(key, holo);
        } else {
            holo.teleport(holoLoc);
        }
        long leftMs = configuredChestCooldownUntil.getOrDefault(key, 0L) - System.currentTimeMillis();
        WeaponRarity rarity = configuredChestRarity.get(key);
        if (leftMs > 0L) {
            long sec = Math.max(1L, (long) Math.ceil(leftMs / 1000.0));
            holo.customName(Component.text("LootChest", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text(" • ", NamedTextColor.DARK_GRAY))
                    .append(rarityDisplay(rarity))
                    .append(Component.text(" • " + sec + "s", NamedTextColor.YELLOW)));
        } else {
            holo.customName(Component.text("LootChest", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text(" • ", NamedTextColor.DARK_GRAY))
                    .append(rarityDisplay(rarity))
                    .append(Component.text(" • Pret", NamedTextColor.GREEN)));
        }
    }

    private static Location parseLocKey(String key) {
        String[] split = key.split(";");
        if (split.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(split[0]);
        if (world == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(split[1]);
            int y = Integer.parseInt(split[2]);
            int z = Integer.parseInt(split[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private WeaponProfile rollWeaponForChest(FileConfiguration cfg, Set<WeaponProfile> excluded, WeaponRarity forcedRarity) {
        WeaponRarity targetTier = forcedRarity;
        if (targetTier == null) {
            WeaponRarity rolledTier = rollRarityTier(cfg);
            targetTier = rolledTier;
            // Nerf leger des coffres aleatoires (zone): legendaire tres rare; epique possible.
            if (targetTier == WeaponRarity.LEGENDARY) {
                targetTier = ThreadLocalRandom.current().nextDouble() < 0.85 ? WeaponRarity.RARE : WeaponRarity.EPIC;
            } else if (targetTier == WeaponRarity.EPIC) {
                double d = ThreadLocalRandom.current().nextDouble();
                if (d < 0.55) {
                    targetTier = WeaponRarity.EPIC;
                } else if (d < 0.82) {
                    targetTier = WeaponRarity.RARE;
                } else {
                    targetTier = WeaponRarity.UNCOMMON;
                }
            } else if (targetTier == WeaponRarity.RARE) {
                targetTier = ThreadLocalRandom.current().nextDouble() < 0.45 ? WeaponRarity.UNCOMMON : WeaponRarity.RARE;
            }
        }
        Set<WeaponProfile> poolSet = new LinkedHashSet<>();
        for (String id : cfg.getStringList("loot.chest-weapons")) {
            WeaponProfile p = WeaponProfile.fromId(id);
            if (p != null && p.rarity() != WeaponRarity.GHOST) {
                poolSet.add(p);
            }
        }
        List<WeaponProfile> pool = new ArrayList<>(poolSet);
        // Si la config est trop pauvre, on élargit le pool pour eviter les coffres monotones.
        if (pool.size() <= 1) {
            for (WeaponProfile profile : WeaponProfile.values()) {
                if (profile.rarity() != WeaponRarity.GHOST) {
                    pool.add(profile);
                }
            }
        }
        if (pool.isEmpty()) {
            pool.addAll(Arrays.asList(WeaponProfile.values()));
        }
        WeaponRarity finalTier = targetTier;
        List<WeaponProfile> match = pool.stream()
                .filter(p -> p.rarity() == finalTier)
                .filter(p -> excluded == null || !excluded.contains(p))
                .toList();
        List<WeaponProfile> pick = match;
        if (pick.isEmpty()) {
            pick = pool.stream()
                    .filter(p -> excluded == null || !excluded.contains(p))
                    .toList();
        }
        if (pick.isEmpty()) {
            pick = pool;
        }
        return pick.get(ThreadLocalRandom.current().nextInt(pick.size()));
    }

    private static WeaponRarity rollConfiguredChestRarity(FileConfiguration cfg) {
        int c = Math.max(0, cfg.getInt("loot.configured-chest-rarity-weights.COMMON", 55));
        int u = Math.max(0, cfg.getInt("loot.configured-chest-rarity-weights.UNCOMMON", 27));
        int r = Math.max(0, cfg.getInt("loot.configured-chest-rarity-weights.RARE", 13));
        int e = Math.max(0, cfg.getInt("loot.configured-chest-rarity-weights.EPIC", 5));
        int total = c + u + r + e;
        if (total <= 0) {
            return WeaponRarity.COMMON;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        if (roll < c) {
            return WeaponRarity.COMMON;
        }
        roll -= c;
        if (roll < u) {
            return WeaponRarity.UNCOMMON;
        }
        roll -= u;
        if (roll < r) {
            return WeaponRarity.RARE;
        }
        return WeaponRarity.EPIC;
    }

    private static Component rarityDisplay(WeaponRarity rarity) {
        if (rarity == null) {
            return Component.text("• ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("Aléatoire", NamedTextColor.GRAY));
        }
        return switch (rarity) {
            case COMMON -> Component.text("Commun", NamedTextColor.WHITE);
            case UNCOMMON -> Component.text("Peu commun", NamedTextColor.GREEN);
            case RARE -> Component.text("Rare", NamedTextColor.AQUA);
            case EPIC -> Component.text("Epique", NamedTextColor.LIGHT_PURPLE);
            case LEGENDARY -> Component.text("Legendaire", NamedTextColor.GOLD);
            case MYTHIC -> Component.text("Mythique", net.kyori.adventure.text.format.TextColor.color(0xFF3D9A));
            case GHOST -> Component.text("Ghost", net.kyori.adventure.text.format.TextColor.color(0x9B6BFF));
        };
    }

    private static WeaponRarity rollRarityTier(FileConfiguration cfg) {
        int c = Math.max(0, cfg.getInt("loot.rarity-weights.COMMON", 48));
        int u = Math.max(0, cfg.getInt("loot.rarity-weights.UNCOMMON", 28));
        int r = Math.max(0, cfg.getInt("loot.rarity-weights.RARE", 14));
        int e = Math.max(0, cfg.getInt("loot.rarity-weights.EPIC", 7));
        int l = Math.max(0, cfg.getInt("loot.rarity-weights.LEGENDARY", 3));
        int m = Math.max(0, cfg.getInt("loot.rarity-weights.MYTHIC", 1));
        int total = c + u + r + e + l + m;
        if (total <= 0) {
            return WeaponRarity.COMMON;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        if (roll < c) {
            return WeaponRarity.COMMON;
        }
        roll -= c;
        if (roll < u) {
            return WeaponRarity.UNCOMMON;
        }
        roll -= u;
        if (roll < r) {
            return WeaponRarity.RARE;
        }
        roll -= r;
        if (roll < e) {
            return WeaponRarity.EPIC;
        }
        roll -= e;
        if (roll < l) {
            return WeaponRarity.LEGENDARY;
        }
        return WeaponRarity.MYTHIC;
    }

    private void removeChestBlockAtKey(String key) {
        String[] split = key.split(";");
        if (split.length != 4) {
            return;
        }
        World world = Bukkit.getWorld(split[0]);
        if (world == null) {
            return;
        }
        int x = Integer.parseInt(split[1]);
        int y = Integer.parseInt(split[2]);
        int z = Integer.parseInt(split[3]);
        Block b = world.getBlockAt(x, y, z);
        if (b.getType() == Material.CHEST) {
            removeChestBlocks(b);
        }
    }

    private void removeChestBlocks(Block start) {
        if (start.getType() != Material.CHEST) {
            return;
        }
        Set<Block> blocks = new HashSet<>();
        blocks.add(start);
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block n = start.getRelative(face);
            if (n.getType() == Material.CHEST) {
                blocks.add(n);
            }
        }
        for (Block b : blocks) {
            b.setType(Material.AIR);
        }
    }

    private void saveLocation(String path, Location location) {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(path));
        String key = locKey(location);
        if (!list.contains(key)) {
            list.add(key);
            plugin.getConfig().set(path, list);
            plugin.saveConfig();
            if ("loot.weapon-chests".equals(path)) {
                updateConfiguredChestHologram(key);
            }
        }
    }

    private void addHeldWeapon(Player player, String path) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(Component.text("Tiens une arme en main.", NamedTextColor.RED));
            return;
        }
        String weaponId = held.getItemMeta() != null && held.getItemMeta().displayName() != null
                ? PlainTextComponentSerializer.plainText().serialize(held.getItemMeta().displayName())
                : held.getType().name();
        weaponId = weaponId.replace("ARME ", "").trim().toUpperCase(Locale.ROOT);
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(path));
        if (!list.contains(weaponId)) {
            list.add(weaponId);
            plugin.getConfig().set(path, list);
            plugin.saveConfig();
        }
        player.sendMessage(Component.text("Arme ajoutee: " + weaponId, NamedTextColor.GREEN));
    }

    private ItemStack named(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        item.setItemMeta(meta);
        return item;
    }
}
