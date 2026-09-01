package net.paulem.krimson.mobs;

import net.paulem.krimson.mobs.boss.BossController;
import net.paulem.krimson.models.Models;
import net.paulem.krimson.models.blockbench.BlockbenchDisplayModel;
import net.paulem.krimson.models.blockbench.rig.ModelInstance;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.Attributable;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Owns every live custom mob, the same way {@code RigManager} owns every live block rig.
 * Responsible for the three things vanilla's own mob-spawning + client-sync system does for
 * free and we have to do ourselves here: creating the entity, keeping its puppet rig glued
 * to it, and cleaning both up together when it's gone.
 */
public final class CustomMobManager {
    private final Plugin plugin;
    private final Map<UUID, CustomMobInstance> instances = new LinkedHashMap<>();
    private BukkitTask task;
    private long lastTickNanos;

    public CustomMobManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }
        lastTickNanos = System.nanoTime();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (CustomMobInstance instance : new ArrayList<>(instances.values())) {
            instance.disposeVisuals();
        }
        instances.clear();
    }

    private void tick() {
        long now = System.nanoTime();
        float delta = Math.min(0.25F, (now - lastTickNanos) / 1.0E9F);
        lastTickNanos = now;

        List<UUID> gone = new ArrayList<>();
        for (Map.Entry<UUID, CustomMobInstance> entry : instances.entrySet()) {
            CustomMobInstance instance = entry.getValue();
            if (!instance.entity().isValid() || instance.rig().isBroken()) {
                gone.add(entry.getKey());
                continue;
            }
            instance.tick(delta);
        }
        for (UUID id : gone) {
            CustomMobInstance instance = instances.remove(id);
            if (instance != null) {
                instance.disposeVisuals();
            }
        }
    }

    /**
     * Spawns a fresh instance of {@code type} at {@code location}: spawns the real body
     * through the plain Bukkit/Paper API (via {@link CustomMobType#mobClass()}), wipes its
     * vanilla AI, applies attributes, then spawns and attaches its Blockbench rig.
     *
     * <p>The body is a real, unmodified vanilla entity - no NMS subclass of our own is
     * needed. Its vanilla goal selector is cleared through {@link KrimsonGoalAccess}, which
     * is the one place in Krimson that touches an NMS field directly (see its class doc for
     * why that stays safe across Minecraft versions); everything else here is pure Bukkit.
     */
    public <T extends org.bukkit.entity.Mob> CustomMobInstance spawn(CustomMobType<T> type, Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("location has no world");
        }

        T mob = world.spawn(location, type.mobClass(), CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
            entity.setInvisible(true); // the vanilla body is hidden; the rig is what's actually seen
            entity.setPersistent(true);
            KrimsonGoalAccess.clearVanillaGoals(((CraftMob) entity).getHandle()); // Krimson's own AI drives the mob from here, not vanilla's

            if (type.onSpawn() != null) {
                type.onSpawn().accept(entity);
            }
        });

        // Re-assert invisibility through the Bukkit-level API: this forces a metadata sync
        // packet to every player already tracking the entity, in case the flag set above
        // (before the entity was added to the world) didn't make it into the initial spawn
        // snapshot.
        mob.setInvisible(true);

        applyAttributes(mob, type);

        mob.getPersistentDataContainer().set(
                MobKeys.CUSTOM_MOB.key(), MobKeys.CUSTOM_MOB.type(), (byte) 1);
        mob.getPersistentDataContainer().set(
                MobKeys.MOB_TYPE.key(), MobKeys.MOB_TYPE.type(), type.getKey().toString());

        BlockbenchDisplayModel model = (BlockbenchDisplayModel) Models.REGISTRY.getOrThrow(type.modelKey());
        ModelInstance rig = model.spawn(location.clone());
        rig.setScale(type.modelScale());

        BossController boss = null;
        if (type.isBoss()) {
            boss = new BossController(mob, type.bossSettings());
        }

        CustomMobInstance instance = new CustomMobInstance(
                type, mob, rig, boss, new net.paulem.krimson.mobs.ai.KrimsonGoalSelector(type.aiGoals()));
        instances.put(mob.getUniqueId(), instance);
        return instance;
    }

    private void applyAttributes(org.bukkit.entity.Mob mob, CustomMobType<?> type) {
        if (!(mob instanceof Attributable attributable)) {
            return;
        }
        for (Map.Entry<Attribute, Double> entry : type.attributeOverrides().entrySet()) {
            var instance = attributable.getAttribute(entry.getKey());
            if (instance != null) {
                instance.setBaseValue(entry.getValue());
            }
        }
        // Attributes like max health only take effect for current health once re-applied.
        if (type.attributeOverrides().containsKey(Attribute.MAX_HEALTH)) {
            mob.setHealth(mob.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
    }

    @Nullable
    public CustomMobInstance instanceOf(Entity entity) {
        return instances.get(entity.getUniqueId());
    }

    public boolean isCustomMob(Entity entity) {
        return instances.containsKey(entity.getUniqueId());
    }

    public void remove(Entity entity, boolean killEntity) {
        CustomMobInstance instance = instances.remove(entity.getUniqueId());
        if (instance != null) {
            instance.disposeVisuals();
        }
        if (killEntity && entity.isValid()) {
            entity.remove();
        }
    }

    public Collection<CustomMobInstance> instances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public static CustomMobManager getInstance() {
        return CustomMobs.MANAGER;
    }
}
