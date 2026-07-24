package net.paulem.krimson.entities;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.entities.custom.CustomEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks all custom entities in the world and manages their lifecycles
 */
public class CustomEntityTracker implements Listener {
    private final Map<UUID, CustomEntity> entities = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public CustomEntityTracker() {
        // Register events
        Bukkit.getPluginManager().registerEvents(this, KrimsonPlugin.getInstance());

        // Start ticking
        startTicking();
    }

    /**
     * Start the entity ticking system
     */
    private void startTicking() {
        tickTask = Bukkit.getScheduler().runTaskTimer(KrimsonPlugin.getInstance(), this::tickEntities, 1L, 1L);
    }

    /**
     * Stop the entity ticking system
     */
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
        }
    }

    /**
     * Tick all entities
     */
    private void tickEntities() {
        entities.values().removeIf(entity -> {
            try {
                entity.tick();
                return !entity.isValid(); // Remove invalid entities
            } catch (Exception e) {
                KrimsonPlugin.getInstance().getLogger().warning("Error ticking entity " + entity.getKey() + ": " + e.getMessage());
                return true; // Remove entities that cause errors
            }
        });
    }

    /**
     * Register a custom entity
     */
    public void registerEntity(CustomEntity entity) {
        if (entity.getEntity() != null) {
            entities.put(entity.getEntity().getUniqueId(), entity);
        }
    }

    /**
     * Remove a custom entity
     */
    public void removeEntity(CustomEntity entity) {
        if (entity.getEntity() != null) {
            entities.remove(entity.getEntity().getUniqueId());
        }
    }

    /**
     * Get a custom entity by entity ID
     */
    public CustomEntity getEntity(Entity entity) {
        return entities.get(entity.getUniqueId());
    }

    /**
     * Check if an entity is a custom entity
     */
    public boolean isCustomEntity(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;

        return entity.getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey("krimson", "custom_entity"),
            PersistentDataType.BYTE
        );
    }

    /**
     * Handle entity interaction
     */
    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (isCustomEntity(entity)) {
            CustomEntity customEntity = getEntity(entity);
            if (customEntity != null) {
                customEntity.onInteract(event);
            }
        }
    }

    /**
     * Handle entity damage
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (isCustomEntity(entity)) {
            CustomEntity customEntity = getEntity(entity);
            if (customEntity != null) {
                customEntity.onDamage(event);

                if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent damageByEntityEvent) {
                    customEntity.onDamageByEntity(damageByEntityEvent);
                }
            }
        }
    }

    /**
     * Handle entity death
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (isCustomEntity(entity)) {
            CustomEntity customEntity = getEntity(entity);
            if (customEntity != null) {
                customEntity.onDeath(event);
                removeEntity(customEntity);
            }
        }
    }

    /**
     * Handle chunk load (for entity persistence)
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // TODO: Implement entity loading from chunk data
    }

    /**
     * Handle chunk unload (for entity persistence)
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // TODO: Implement entity saving to chunk data
    }

    /**
     * Get all registered entities
     */
    public Map<UUID, CustomEntity> getAllEntities() {
        return new HashMap<>(entities);
    }

    /**
     * Clear all entities
     */
    public void clearAll() {
        entities.values().forEach(CustomEntity::remove);
        entities.clear();
    }
}