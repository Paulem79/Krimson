package net.paulem.krimson.packets.entity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns every {@link VirtualEntity} currently active and keeps each one's viewer set in
 * sync with which players are actually nearby, the same way {@code CustomMobManager}
 * ticks real mobs. Reused across the Blockbench rig system and BDEngine models.
 */
public final class VirtualEntityManager {
    private static final VirtualEntityManager INSTANCE = new VirtualEntityManager();

    /** Default radius (blocks) within which a player is sent this entity's packets. */
    public static final double DEFAULT_VIEW_RADIUS = 48.0;

    private final Map<Integer, VirtualEntity> entities = new LinkedHashMap<>();
    private Plugin plugin;
    private BukkitTask task;

    private VirtualEntityManager() {
    }

    public static VirtualEntityManager getInstance() {
        return INSTANCE;
    }

    public void start(Plugin plugin) {
        this.plugin = plugin;
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (VirtualEntity entity : entities.values()) {
            entity.remove();
        }
        entities.clear();
    }

    public void register(VirtualEntity entity) {
        entities.put(entity.entityId(), entity);
    }

    public void unregister(VirtualEntity entity) {
        entity.remove();
        entities.remove(entity.entityId());
    }

    /** Drops a player from every tracked entity's viewer set without sending packets (e.g. on quit). */
    public void handleQuit(UUID playerId) {
        for (VirtualEntity entity : entities.values()) {
            entity.dropViewer(playerId);
        }
    }

    private void tick() {
        if (entities.isEmpty()) {
            return;
        }
        for (VirtualEntity entity : entities.values()) {
            Location location = entity.location();
            if (location.getWorld() == null) {
                continue;
            }
            double radiusSquared = DEFAULT_VIEW_RADIUS * DEFAULT_VIEW_RADIUS;
            for (Player player : location.getWorld().getPlayers()) {
                boolean inRange = player.getLocation().distanceSquared(location) <= radiusSquared;
                boolean isViewer = entity.hasViewer(player);
                if (inRange && !isViewer) {
                    entity.addViewer(player);
                } else if (!inRange && isViewer) {
                    entity.removeViewer(player);
                }
            }
        }
    }
}
