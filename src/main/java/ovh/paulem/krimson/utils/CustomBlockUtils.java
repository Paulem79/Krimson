package ovh.paulem.krimson.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.blocks.CustomBlock;

public class CustomBlockUtils {
    /**
     * Handles suppression of custom blocks.
     * Removes the custom block entity if it is associated with the given block.
     *
     * @param block the block to check for custom block suppression
     */
    public static void handleBlockSuppression(Block block, @Nullable Event event) {
        System.out.println(block.getType() + " at " + block.getLocation() + " is a custom block, handling suppression...");
        System.out.println(event);
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
    public static CustomBlock getCustomBlockFromLoc(Location location) {
        Block block = location.getBlock();

        return Krimson.customBlocks.getBlockAt(block);
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
