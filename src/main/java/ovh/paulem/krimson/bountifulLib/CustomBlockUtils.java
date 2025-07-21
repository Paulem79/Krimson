package ovh.paulem.krimson.bountifulLib;

import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.bountifulLib.blocks.CustomBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.Nullable;

public class CustomBlockUtils {
    /**
     * Handles suppression of custom blocks.
     * Removes the custom block entity if it is associated with the given block.
     *
     * @param block the block to check for custom block suppression
     */
    public static void handleBlockSuppression(Block block, @Nullable BlockEvent event) {
        Entity entity = getDisplayFromBlock(block);

        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity) && getBlockFromDisplay(entity).getLocation().equals(block.getLocation())) {
            if(event != null){
                CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);
                if(customBlock != null){
                    if(event instanceof BlockBreakEvent){
                        customBlock.onPlayerBreak((BlockBreakEvent) event);
                    } else
                    {
                        customBlock.onBreak(event);
                    }
                }
            }

            removeDisplay((ItemDisplay) entity);

            if(!entity.isDead()) {
                entity.remove();
            }
        }
    }

    /**
     * Gets the block from the given entity.
     * @param entity the entity to get the block from
     * @return the block if found, otherwise null
     */
    @Nullable
    public static Block getBlockFromDisplay(@Nullable Entity entity) {
        if(entity == null) return null;

        return entity.getLocation().subtract(.5, .5, .5).getBlock();
    }

    /**
     * Gets the custom block display entity from the given block.
     * @param block the block to get the display entity from
     * @return the display entity if found, otherwise null
     */
    @Nullable
    public static Entity getDisplayFromBlock(Block block) {
        @Nullable Entity entity = null;
        for (Entity nearbyEntity : block.getWorld().getNearbyEntities(block.getLocation().add(0.5,0.5,0.5) ,0.1 ,0.1, 0.1)) {
            if(entity == null) {
                entity = nearbyEntity;
            }

            Location blockLocationForNearby = nearbyEntity.getLocation().subtract(.5, .5, .5);
            double nearbyDistance = blockLocationForNearby.distance(block.getLocation());

            if(blockLocationForNearby.getBlock().getLocation().equals(block.getLocation()) && nearbyDistance < entity.getLocation().distance(block.getLocation())) {
                entity = nearbyEntity;
            }
        }

        return entity;
    }

    /**
     * Gets the custom block from the given entity.
     * @param entity the entity to get the custom block from
     * @return the custom block if found, otherwise null
     */
    @Nullable
    public static CustomBlock getCustomBlockFromEntity(Entity entity) {
        return Krimson.customBlocks
                .stream()
                .filter(cB -> cB.getSpawnedDisplay().equals(entity))
                .findFirst()
                .orElse(null);
    }

    /**
     * Removes the custom block display and updates the customBlocks list.
     *
     * @param itemDisplay the custom block display to remove
     */
    public static void removeDisplay(ItemDisplay itemDisplay) {
        itemDisplay.getWorld().getBlockAt(itemDisplay.getLocation()).setType(Material.AIR);
        itemDisplay.getWorld().getBlockAt(itemDisplay.getLocation().add(0, 1, 0)).setType(Material.AIR);

        Krimson.customBlocks.removeIf(customBlock -> customBlock.getSpawnedDisplay().equals(itemDisplay));
    }
}
