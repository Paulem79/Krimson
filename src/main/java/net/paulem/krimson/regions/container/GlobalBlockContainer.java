package net.paulem.krimson.regions.container;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.regions.BlockHolder;

import java.util.*;

public class GlobalBlockContainer {

    private final Map<UUID, WorldBlockContainer> worldContainers;

    private GlobalBlockContainer() {
        this.worldContainers = new HashMap<>();
    }

    public static GlobalBlockContainer of() {
        return new GlobalBlockContainer();
    }

    @Nullable
    public WorldBlockContainer getWorldContainer(UUID worldId) {
        return worldContainers.get(worldId);
    }

    public WorldBlockContainer getOrCreateWorldContainer(UUID worldId) {
        WorldBlockContainer container = getWorldContainer(worldId);

        if (container == null) {
            container = WorldBlockContainer.of(worldId);
            worldContainers.put(worldId, container);
        }

        return container;
    }

    public void removeWorldContainer(UUID worldId) {
        worldContainers.remove(worldId);
    }

    @Nullable
    public WorldBlockContainer getWorldContainer(World world) {
        return getWorldContainer(world.getUID());
    }

    public WorldBlockContainer getOrCreateWorldContainer(World world) {
        return getOrCreateWorldContainer(world.getUID());
    }

    @Nullable
    public <T> T getBlock(Location location) {
        WorldBlockContainer container = getWorldContainer(location.getWorld());

        if (container == null) {
            return null;
        }

        return container.getBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public <T> void setBlock(Location location, T block) {
        WorldBlockContainer container = getOrCreateWorldContainer(location.getWorld());
        container.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    public void removeBlock(Location location) {
        WorldBlockContainer container = getWorldContainer(location.getWorld());

        if (container == null) {
            return;
        }

        container.removeBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public List<BlockHolder<?>> getAllBlocks() {
        List<BlockHolder<?>> holders = new ArrayList<>();

        for (WorldBlockContainer container : worldContainers.values()) {
            holders.addAll(container.getAllBlocks());
        }

        return holders;
    }

    public <T> List<? extends T> getAllBlocks(Class<T> type) {
        List<T> blocks = new ArrayList<>();

        for (BlockHolder<?> holder : getAllBlocks()) {
            if (type.isInstance(holder.getData())) {
                blocks.add(type.cast(holder.getData()));
            }
        }

        return blocks;
    }
}