package ovh.paulem.krimson.blocks;

import com.google.common.base.Preconditions;
import lombok.Getter;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.function.Predicate;

public abstract class CustomBlock {
    public static final Vector3f OFFSET = new Vector3f(.0005f);

    @Getter
    private final Material blockInside;
    @Getter
    private final ItemStack displayedItem;
    @Getter
    private final int emittingLightLevel;
    @Getter
    private ItemDisplay spawnedDisplay;

    private final Predicate<CustomBlock> commonArguments;

    /**
     * Set the brightness of the block, using an asynchronous task in the main file
     */
    public static final Predicate<CustomBlock> tickPredicate = customBlock -> {
        ItemDisplay itemDisplay = customBlock.spawnedDisplay;

        if(itemDisplay == null) {
            Krimson.customBlocks.remove(customBlock);
            return false;
        }

        Block block = CustomBlockUtils.getBlockFromDisplay(itemDisplay);
        if (block.getType() != customBlock.blockInside) {
            Krimson.getScheduler().runTask(() -> {
                CustomBlockUtils.handleBlockSuppression(block, null);
            });
        }

        if(Krimson.getConfiguration().getBoolean("preciseLightning", true)) {
            // Precise lightning : check the light level of the block in all cartesian directions
            byte maxLight = 0;
            for (BlockFace blockFace : BlockFace.values()) {
                if(!blockFace.isCartesian()) {
                    continue;
                }

                byte actualLight = itemDisplay.getLocation().getBlock().getRelative(blockFace).getLightFromSky();
                if(actualLight > maxLight) {
                    maxLight = actualLight;
                }
            }
            itemDisplay.setBrightness(new Display.Brightness(customBlock.emittingLightLevel, maxLight));
        } else
        {
            // Normal lightning : check the light level of the block above the item
            itemDisplay.setBrightness(new Display.Brightness(customBlock.emittingLightLevel, itemDisplay.getLocation().getBlock().getRelative(BlockFace.UP).getLightFromSky()));
        }

        return true;
    };

    /**
     * Create a custom block with the given item<br><br>
     * To get the custom block from the item display, you can use {@link CustomBlockUtils#getCustomBlockFromEntity(Entity)}
     *
     * @param displayedItem The item to display
     */
    public CustomBlock(Material blockInside, ItemStack displayedItem, int emittingLightLevel) {
        Preconditions.checkArgument(blockInside.isBlock(), "The material inside must be a block!");
        this.blockInside = blockInside;
        this.displayedItem = displayedItem;
        this.emittingLightLevel = emittingLightLevel;

        commonArguments = customBlock -> {
            ItemDisplay itemDisplay = customBlock.spawnedDisplay;
            itemDisplay.setItemStack(displayedItem);

            Transformation actualTransformation = itemDisplay.getTransformation();
            itemDisplay.setTransformation(new Transformation(
                    actualTransformation.getTranslation(),
                    actualTransformation.getLeftRotation(),
                    actualTransformation.getScale().add(OFFSET),
                    actualTransformation.getRightRotation()
            ));

            itemDisplay.getPersistentDataContainer().set(Krimson.customBlockKey, PersistentDataType.BYTE, (byte) 1);
            Krimson.customBlocks.add(customBlock);

            tickPredicate.test(customBlock);

            return true;
        };
    }

    /**
     * Create a custom block with the given item<br><br>
     * To get the custom block from the item display, you can use {@link CustomBlockUtils#getCustomBlockFromEntity(Entity)}
     *
     * @param displayedItem The item to display
     */
    public CustomBlock(Material blockInside, ItemStack displayedItem, int emittingLightLevel, ItemDisplay itemDisplay) {
        this(blockInside, displayedItem, emittingLightLevel);
        this.spawnedDisplay = itemDisplay;
    }

    /**
     * Spawn the custom block at the given location
     * @param blockLoc The location of the block
     */
    public void spawn(Location blockLoc) {
        if(blockLoc.getWorld() == null) {
            return;
        }

        blockLoc.getBlock().setType(blockInside);

        if(displayedItem.getType() == Material.PLAYER_HEAD)
        {
            // HEAD
            blockLoc.getWorld().spawn(blockLoc.add(.5, 0 + OFFSET.y(), .5), ItemDisplay.class, itemDisplay -> {
                spawnedDisplay = itemDisplay;

                itemDisplay.setRotation(180F, 0F);

                itemDisplay.setTransformation(new Transformation(
                        new Vector3f(0f, 1f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(2f).add(OFFSET.mul(2)),
                        new AxisAngle4f(0f, 0f, 0f, 1f)
                ));

                commonArguments.test(this);
            });
        } else
        {
            // BLOCK
            blockLoc.getWorld().spawn(blockLoc.add(.5, .5, .5), ItemDisplay.class, itemDisplay -> {
                spawnedDisplay = itemDisplay;

                itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);

                itemDisplay.setTransformation(new Transformation(
                        new Vector3f(0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(1f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)
                ));

                commonArguments.test(this);
            });
        }

        // TODO : Wtf not working, it blocks everything
        // Spawn the light
        Block lightBlock = blockLoc.add(0, 1, 0).getBlock();
        if(lightBlock.isEmpty()) {
            lightBlock.setType(Material.LIGHT);
            Light light = (Light) lightBlock.getBlockData();
            light.setLevel(emittingLightLevel);
            lightBlock.setBlockData(light);
            lightBlock.getState().update();
        }

        Krimson.trackedDisplays.add(spawnedDisplay);
    }

    /**
     * Called when a player interacts with the custom block
     */
    public void onInteract(PlayerInteractEvent event) {
    }
    /**
     * Called when the custom block is placed by a player.
     */
    public void onPlace(BlockPlaceEvent event) {
    }
    /**
     * Called when the custom block is broken by a player.
     */
    public void onPlayerBreak(BlockBreakEvent event) {
        onBreak(event);
    };
    /**
     * Called when the custom block is broken. (called also when a player breaks the block)
     */
    public void onBreak(BlockEvent event) {
    }
}
