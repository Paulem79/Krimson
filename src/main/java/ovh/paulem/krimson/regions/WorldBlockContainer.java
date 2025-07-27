package ovh.paulem.krimson.regions;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WorldBlockContainer {

    private final UUID worldId;
    private final Map<ChunkKey, ChunkBlockContainer> chunkContainers;

    private WorldBlockContainer(UUID worldId) {
        this.worldId = worldId;
        this.chunkContainers = new HashMap<>();
    }

    public static WorldBlockContainer of(UUID worldId) {
        return new WorldBlockContainer(worldId);
    }

    public static WorldBlockContainer of(World world) {
        return of(world.getUID());
    }

    public UUID getWorldId() {
        return worldId;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldId);
    }

    @Nullable
    public ChunkBlockContainer getChunkContainer(ChunkKey key) {
        return chunkContainers.get(key);
    }

    public ChunkBlockContainer getOrCreateChunkContainer(ChunkKey key) {
        ChunkBlockContainer container = getChunkContainer(key);

        if (container == null) {
            container = ChunkBlockContainer.of(this, key);
            chunkContainers.put(key, container);
        }

        return container;
    }

    public void removeChunkContainer(ChunkKey key) {
        chunkContainers.remove(key);
    }

    public void removeChunkContainer(Chunk chunk) {
        removeChunkContainer(ChunkKey.fromChunk(chunk));
    }

    @Nullable
    public <T> T getBlock(int x, int y, int z) {
        ChunkKey key = ChunkKey.fromCoordinates(x, z);
        ChunkBlockContainer container = getChunkContainer(key);

        if (container == null) {
            return null;
        }

        return container.getBlock(x, y, z);
    }

    public <T> void setBlock(int x, int y, int z, T block) {
        ChunkKey key = ChunkKey.fromCoordinates(x, z);
        ChunkBlockContainer container = getOrCreateChunkContainer(key);

        container.setBlock(x, y, z, block);
    }

    public void removeBlock(int x, int y, int z) {
        ChunkKey key = ChunkKey.fromCoordinates(x, z);
        ChunkBlockContainer container = getChunkContainer(key);

        if (container == null) {
            return;
        }

        container.removeBlock(x, y, z);
    }

    public Collection<BlockHolder<?>> getAllBlocks() {
        List<BlockHolder<?>> holders = new ArrayList<>();

        for (ChunkBlockContainer container : chunkContainers.values()) {
            holders.addAll(container.getAllBlocks());
        }

        return holders;
    }
}