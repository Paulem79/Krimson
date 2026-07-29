package net.paulem.krimson.models.blockbench;

import net.paulem.krimson.models.blockbench.model.BbModel;
import net.paulem.krimson.models.blockbench.rig.ModelInstance;
import net.paulem.krimson.models.blockbench.rig.RigManifest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector3f;

import java.util.*;

/** Owns every spawned rig and drives them from a single repeating task. */
public final class RigManager {
    private final Plugin plugin;
    private final BbModel model;
    private final RigManifest manifest;
    private final Material carrier;
    private final int periodTicks;
    private final Vector3f originOffset;

    // TODO: Not every instances on one rig, like BDEngineModel, it's one instance per rig, so per instance of BlockbenchDisplayModel
    private final Map<UUID, ModelInstance> instances = new LinkedHashMap<>();
    private BukkitTask task;
    private long lastTickNanos;
    private int lastUpdateCount;

    public RigManager(Plugin plugin, BbModel model, RigManifest manifest, Material carrier,
                      int periodTicks, Vector3f originOffset) {
        this.plugin = plugin;
        this.model = model;
        this.manifest = manifest;
        this.carrier = carrier;
        this.periodTicks = periodTicks;
        this.originOffset = originOffset;
    }

    public void start() {
        if (task != null) {
            return;
        }
        lastTickNanos = System.nanoTime();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick,
                periodTicks, periodTicks);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ModelInstance instance : new ArrayList<>(instances.values())) {
            instance.remove();
        }
        instances.clear();
    }

    private void tick() {
        long now = System.nanoTime();
        // Real elapsed time, so animations keep their authored speed even if the
        // server is running behind.
        float delta = Math.min(0.25F, (now - lastTickNanos) / 1.0E9F);
        lastTickNanos = now;

        int updates = 0;
        List<UUID> broken = new ArrayList<>();
        for (Map.Entry<UUID, ModelInstance> entry : instances.entrySet()) {
            ModelInstance instance = entry.getValue();
            if (instance.isBroken()) {
                broken.add(entry.getKey());
                continue;
            }
            updates += instance.tick(delta);
        }
        for (UUID id : broken) {
            ModelInstance instance = instances.remove(id);
            if (instance != null) {
                instance.remove();
            }
        }
        lastUpdateCount = updates;
    }

    /** Spawns a rig */
    public ModelInstance spawnFor(Location location) {
        ModelInstance instance = new ModelInstance(model, manifest, location, location.getYaw(), carrier,
                periodTicks, originOffset);
        instance.spawn();
        instances.put(instance.id, instance);
        return instance;
    }

    public Collection<ModelInstance> instances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    /** The rig nearest to a player, for commands that act on "the" rig. */
    public ModelInstance nearest(Location location) {
        ModelInstance best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ModelInstance instance : instances.values()) {
            Location base = instance.base();
            if (!base.getWorld().equals(location.getWorld())) {
                continue;
            }
            double distance = base.distanceSquared(location);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = instance;
            }
        }
        return best;
    }

    public void removeAll() {
        for (ModelInstance instance : new ArrayList<>(instances.values())) {
            instance.remove();
        }
        instances.clear();
    }

    public int count() {
        return instances.size();
    }

    public int lastUpdateCount() {
        return lastUpdateCount;
    }

    public int periodTicks() {
        return periodTicks;
    }
}
