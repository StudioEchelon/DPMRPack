package fr.dpmr.npc.citizens;

import fr.dpmr.npc.NpcSkins;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Integration optionnelle Citizens : NPC joueur complet (sans dependance compile).
 * Tout passe par reflexion + classloader du plugin Citizens.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class CitizensCombatNpcBridge {

    private CitizensCombatNpcBridge() {
    }

    public static boolean isCitizensPresent() {
        Plugin p = Bukkit.getPluginManager().getPlugin("Citizens");
        return p != null && p.isEnabled();
    }

    /**
     * @param entity n'importe quelle entite (ex: joueur NPC)
     */
    public static boolean isCitizensNpcEntity(org.bukkit.entity.Entity entity) {
        if (!isCitizensPresent() || entity == null) {
            return false;
        }
        try {
            ClassLoader cl = Bukkit.getPluginManager().getPlugin("Citizens").getClass().getClassLoader();
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI", true, cl);
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            Object npc = registry.getClass().getMethod("getNPC", org.bukkit.entity.Entity.class).invoke(registry, entity);
            return npc != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void destroyNpc(Object npc) {
        if (npc == null) {
            return;
        }
        tryDespawnCitizensNpc(npc);
        try {
            npc.getClass().getMethod("destroy").invoke(npc);
        } catch (Throwable ignored) {
        }
    }

    private static void tryDespawnCitizensNpc(Object npc) {
        try {
            npc.getClass().getMethod("despawn").invoke(npc);
            return;
        } catch (Throwable ignored) {
        }
        ClassLoader cl = npc.getClass().getClassLoader();
        String[] reasonClasses = {
                "net.citizensnpcs.api.npc.NPC$DespawnReason",
                "net.citizensnpcs.api.event.DespawnReason"
        };
        String[] reasonNames = {"PLUGIN", "REMOVAL", "PENDING_RESPAWN"};
        for (String rcn : reasonClasses) {
            try {
                Class<?> rc = Class.forName(rcn, true, cl);
                for (String name : reasonNames) {
                    try {
                        Object reason = Enum.valueOf((Class) rc, name);
                        npc.getClass().getMethod("despawn", rc).invoke(npc, reason);
                        return;
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** Detruit le NPC Citizens attache a cette entite (ex. joueur PNJ de combat). */
    public static void destroyNpcForEntity(org.bukkit.entity.Entity entity) {
        if (!isCitizensPresent() || entity == null) {
            return;
        }
        try {
            ClassLoader cl = Bukkit.getPluginManager().getPlugin("Citizens").getClass().getClassLoader();
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI", true, cl);
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            Object npc = registry.getClass().getMethod("getNPC", org.bukkit.entity.Entity.class).invoke(registry, entity);
            destroyNpc(npc);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Cree un NPC joueur Citizens de combat, ou null si echec (fallback ArmorStand).
     */
    public static CitizensSpawnResult trySpawn(JavaPlugin plugin, Location spawnLoc, String displayName,
            String kindName, ItemStack mainWeapon, ItemStack helmet) {
        if (!isCitizensPresent()) {
            return null;
        }
        Plugin cit = Bukkit.getPluginManager().getPlugin("Citizens");
        if (cit == null) {
            return null;
        }
        ClassLoader cl = cit.getClass().getClassLoader();
        try {
            Class<?> apiClass = Class.forName("net.citizensnpcs.api.CitizensAPI", true, cl);
            Object registry = apiClass.getMethod("getNPCRegistry").invoke(null);
            Method createNpc = registry.getClass().getMethod("createNPC", EntityType.class, String.class, Location.class);
            Object npc = createNpc.invoke(registry, EntityType.PLAYER, displayName, spawnLoc);
            if (npc == null) {
                return null;
            }
            invokeIfPresent(npc, "setUseMinecraftAI", new Class<?>[]{boolean.class}, false);
            invokeIfPresent(npc, "setProtected", new Class<?>[]{boolean.class}, false);

            Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
            if (!(entity instanceof LivingEntity living) || !living.isValid()) {
                destroyNpc(npc);
                return null;
            }

            if (living.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                living.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            }
            living.setHealth(20.0);

            applyCitizensEquipment(cl, npc, kindName, displayName, mainWeapon, helmet);

            if (living instanceof Player p) {
                p.setCollidable(true);
                p.setCanPickupItems(false);
                p.playerListName(net.kyori.adventure.text.Component.empty());
            }

            return new CitizensSpawnResult(living, npc);
        } catch (Throwable t) {
            plugin.getLogger().warning("Citizens NPC spawn: " + t.getMessage());
            return null;
        }
    }

    private static void invokeIfPresent(Object npc, String method, Class<?>[] params, Object arg) {
        try {
            Method m = npc.getClass().getMethod(method, params);
            m.invoke(npc, arg);
        } catch (Throwable ignored) {
        }
    }

    private static void applyCitizensEquipment(ClassLoader cl, Object npc, String kindName,
            String displayName, ItemStack mainWeapon, ItemStack helmet) throws Exception {
        Class<?> equipmentClass = Class.forName("net.citizensnpcs.api.trait.trait.Equipment", true, cl);
        Class<?> slotClass = Class.forName("net.citizensnpcs.api.trait.trait.Equipment$EquipmentSlot", true, cl);
        Object trait = npc.getClass().getMethod("getOrAddTrait", Class.class).invoke(npc, equipmentClass);
        Method set = trait.getClass().getMethod("set", slotClass, ItemStack.class);

        ItemStack main = mainWeapon != null ? mainWeapon.clone() : new ItemStack(Material.IRON_SWORD);
        ItemStack air = new ItemStack(Material.AIR);
        ItemStack helm = helmet != null && helmet.getType() != Material.AIR
                ? helmet.clone()
                : NpcSkins.randomHeadForKind(kindName, UUID.randomUUID());

        set.invoke(trait, Enum.valueOf((Class) slotClass, "HELMET"), helm);
        set.invoke(trait, Enum.valueOf((Class) slotClass, "CHESTPLATE"), air);
        set.invoke(trait, Enum.valueOf((Class) slotClass, "LEGGINGS"), air);
        set.invoke(trait, Enum.valueOf((Class) slotClass, "BOOTS"), air);
        set.invoke(trait, Enum.valueOf((Class) slotClass, "HAND"), main);
        set.invoke(trait, Enum.valueOf((Class) slotClass, "OFF_HAND"), air);
    }

    public record CitizensSpawnResult(LivingEntity living, Object citizensNpcHandle) {
    }
}
