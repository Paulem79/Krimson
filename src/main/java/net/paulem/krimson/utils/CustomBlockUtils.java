package net.paulem.krimson.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.blocks.custom.CustomBlock;

public class CustomBlockUtils {
    /**
     * Handles suppression of custom blocks.
     * Removes the custom block entity if it is associated with the given block.
     *
     * @param block the block to check for custom block suppression
     */
    public static void handleBlockSuppression(Block block, @Nullable Event event) {
        if (Krimson.isCustomBlockFromWatcher(block)) {
            CustomBlock customBlock = Krimson.customBlocks.getBlockAt(block);
            if (customBlock != null) {
                if (event instanceof BlockBreakEvent) {
                    customBlock.onPlayerBreak((BlockBreakEvent) event);
                } else {
                    customBlock.onBreak(event, null);
                }
            }

            removeBlock(block);
        }
    }

    @Nullable
    public static<T extends CustomBlock> T getCustomBlockFromLoc(@Nullable Location location) {
        if (location == null) {
            return null;
        }

        Block block = location.getBlock();

        return (T) Krimson.customBlocks.getBlockAt(block);
    }

    /**
     * Removes the custom block display and updates the customBlocks list.
     *
     * @param block the custom block's block to remove
     */
    public static void removeBlock(Block block) {
        CustomBlock foundCustomBlock = Krimson.customBlocks.getBlockAt(block);

        if (foundCustomBlock == null) {
            return;
        }

        foundCustomBlock.remove();
    }
}
