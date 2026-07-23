package net.paulem.krimson.regions;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public class ChunkKey {
    private final int x;
    private final int z;

    private ChunkKey(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public static ChunkKey fromChunk(Chunk chunk) {
        return new ChunkKey(chunk.getX(), chunk.getZ());
    }

    public static ChunkKey fromLocation(Location location) {
        return new ChunkKey(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static ChunkKey fromCoordinates(int x, int z) {
        return new ChunkKey(x >> 4, z >> 4);
    }

    public static ChunkKey fromLong(long key) {
        return new ChunkKey((int) (key >> 32), (int) (key & 0xFFFFFFFFL));
    }

    public int getChunkX() {
        return x;
    }

    public int getChunkZ() {
        return z;
    }

    public boolean isInChunk(Chunk chunk) {
        return chunk.getX() == x && chunk.getZ() == z;
    }

    public ChunkKey getRelative(int x, int z) {
        return new ChunkKey(this.x + x, this.z + z);
    }

    public int distanceTo(ChunkKey other) {
        return Math.abs(other.x - x) + Math.abs(other.z - z);
    }

    public int getBlockX() {
        return getChunkX() << 4;
    }

    public int getBlockZ() {
        return getChunkZ() << 4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkKey chunkKey = (ChunkKey) o;
        return x == chunkKey.x && z == chunkKey.z;
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }

    public Chunk toChunk(World world) {
        return world.getChunkAt(x, z);
    }

    public long asLong() {
        return (long) x << 32 | (long) z & 0xFFFFFFFFL;
    }

    @Override
    public String toString() {
        return "ChunkKey{" +
                "x=" + x +
                ", z=" + z +
                '}';
    }
}