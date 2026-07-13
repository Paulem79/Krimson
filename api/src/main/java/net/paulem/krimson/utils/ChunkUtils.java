package net.paulem.krimson.utils;

import net.paulem.krimson.common.KrimsonPlugin;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import net.paulem.krimson.regions.ChunkKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ChunkUtils {
    public static final int VIEW_DISTANCE = KrimsonPlugin.getConfiguration().getInt("view-distance", 6);

    public static Collection<Chunk> getChunksAroundPlayer(Player player) {
        World world = player.getWorld();
        int baseX = player.getLocation().getChunk().getX();
        int baseZ = player.getLocation().getChunk().getZ();

        int viewDistance = Math.min(player.getClientViewDistance(), VIEW_DISTANCE);
        int radiusViewDistance = viewDistance / 2;

        Set<Chunk> chunksAroundPlayer = new HashSet<>();
        for (int x = -radiusViewDistance; x <= radiusViewDistance; x++) {
            for (int z = -radiusViewDistance; z <= radiusViewDistance; z++) {
                Chunk chunk = world.getChunkAt(baseX + x, baseZ + z);
                chunksAroundPlayer.add(chunk);
            }
        }

        return chunksAroundPlayer;
    }

    public static Collection<ChunkKey> getActiveChunks(World world) {
        Set<ChunkKey> result = new HashSet<>();

        for (Player player : world.getPlayers()) {
            for (Chunk chunk : ChunkUtils.getChunksAroundPlayer(player)) {
                if (!chunk.isLoaded()) {
                    continue;
                }

                result.add(ChunkKey.fromChunk(chunk));
            }
        }

        return result;
    }
}
