package ovh.paulem.krimson.regions;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.blocks.custom.CustomBlock;
import ovh.paulem.krimson.blocks.custom.CustomBlockTypeChecker;
import ovh.paulem.krimson.regions.container.ChunkBlockContainer;
import ovh.paulem.krimson.regions.container.GlobalBlockContainer;
import ovh.paulem.krimson.regions.container.WorldBlockContainer;
import ovh.paulem.krimson.utils.ChunkUtils;
import ovh.paulem.krimson.utils.PersistentDataUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CustomBlockTracker {
    @Getter
    private final GlobalBlockContainer globalContainer = GlobalBlockContainer.of();
    @Getter
    private final Map<Boolean, Integer> lastTickedCount = new HashMap<>();

    public CustomBlockTracker() {
        Krimson.getScheduler().runTaskTimerAsynchronously(() -> tickBlocks(CustomBlock::tickAsync, true), 1, 1);

        Krimson.getScheduler().runTaskTimer(() -> tickBlocks(CustomBlock::tickSync, false), 1, 1);
    }

    @Nullable
    public CustomBlock getBlockAt(Block block) {
        return globalContainer.getBlock(block.getLocation());
    }

    public void registerBlock(CustomBlock block) {
        globalContainer.setBlock(block.getPosition(), block);
    }

    public void removeBlock(CustomBlock block) {
        globalContainer.removeBlock(block.getPosition());
    }

    public void handleChunkLoad(Chunk chunk) {
        for (Block block : PersistentDataUtils.getBlocksWithCustomData(Krimson.getInstance(), chunk, Krimson::isCustomBlock)) {
            CustomBlock customBlock = new CustomBlockTypeChecker(block).get();
            registerBlock(customBlock);
        }
    }

    public void handleChunkUnload(Chunk chunk) {
        saveChunk(chunk, holder -> {
            CustomBlock customBlock = (CustomBlock) holder.getData();

            customBlock.onUnload();
        });

        WorldBlockContainer container = globalContainer.getWorldContainer(chunk.getWorld());

        if (container == null) {
            return;
        }

        container.removeChunkContainer(chunk);
    }

    public void saveChunk(Chunk chunk) {
        saveChunk(chunk, holder -> {
            CustomBlock customBlock = (CustomBlock) holder.getData();

            customBlock.onUnload();
        });
    }

    public void saveChunk(Chunk chunk, Consumer<BlockHolder<?>> callback) {
        World world = chunk.getWorld();
        ChunkKey key = ChunkKey.fromChunk(chunk);
        WorldBlockContainer container = globalContainer.getWorldContainer(world);

        if (container == null) {
            return;
        }

        ChunkBlockContainer chunkContainer = container.getChunkContainer(key);

        if (chunkContainer == null) {
            return;
        }

        for (BlockHolder<?> holder : chunkContainer.getAllBlocks()) {
            CustomBlock block = (CustomBlock) holder.getData();
            saveBlock(block);

            callback.accept(holder);
        }
    }

    private <T> void saveBlock(CustomBlock block) {
        block.onUnload();
    }

    private void tickBlocks(Consumer<CustomBlock> callback, boolean async) {
        int tickCount = 0;

        for (World world : Bukkit.getWorlds()) {
            WorldBlockContainer worldContainer = getGlobalContainer().getWorldContainer(world);

            if (worldContainer == null) {
                continue;
            }

            for (ChunkKey key : ChunkUtils.getActiveChunks(world)) {
                ChunkBlockContainer chunkContainer = worldContainer.getChunkContainer(key);

                if (chunkContainer == null) {
                    continue;
                }

                for (BlockHolder<?> block : chunkContainer.getAllBlocks()) {
                    if (!(block.getData() instanceof CustomBlock customBlock)) {
                        continue;
                    }

                    callback.accept(customBlock);
                    tickCount++;
                }
            }
        }

        lastTickedCount.put(async, tickCount);
    }
}