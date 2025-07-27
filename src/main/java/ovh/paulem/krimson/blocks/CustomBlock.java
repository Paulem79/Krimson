package ovh.paulem.krimson.blocks;

import com.google.common.base.Preconditions;
import com.jeff_media.customblockdata.CustomBlockData;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.codec.Codecs;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.items.BlockItem;
import ovh.paulem.krimson.items.Items;
import ovh.paulem.krimson.utils.BlockUtils;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import ovh.paulem.krimson.properties.PropertiesField;
import ovh.paulem.krimson.properties.PropertiesStore;
import ovh.paulem.krimson.utils.NamespacedKeyUtils;

import java.util.Optional;
import java.util.function.Predicate;

public class CustomBlock {
    public static final Vector3f OFFSET = new Vector3f(.0005f);

    @Getter
    protected final NamespacedKey dropIdentifier;
    @Getter
    protected final Material blockInside;
    @Getter
    protected final ItemStack displayedItem;
    @Getter
    protected ItemDisplay spawnedDisplay;
    @Getter
    protected PropertiesStore properties;

    @Getter
    private PropertiesField<String> dropIdentifierField;
    @Getter
    private PropertiesField<String> blockInsideField;
    @Getter
    @Nullable
    private PropertiesField<byte[]> displayedItemField;

    private final Predicate<CustomBlock> commonArguments;

    /**
     * Create a custom block with the given item<br><br>
     * To get the custom block from the item display, you can use {@link CustomBlockUtils#getCustomBlockFromEntity(Entity)}
     *
     * @param displayedItem The item to display
     */
    public CustomBlock(NamespacedKey dropIdentifier, Material blockInside, ItemStack displayedItem) {
        Preconditions.checkArgument(blockInside.isBlock(), "The material inside must be a block!");
        this.dropIdentifier = dropIdentifier;
        this.blockInside = blockInside;
        this.displayedItem = displayedItem;

        commonArguments = customBlock -> {
            ItemDisplay itemDisplay = customBlock.spawnedDisplay;
            itemDisplay.setItemStack(displayedItem);
            setDisplayAndProperties(itemDisplay);

            Transformation actualTransformation = itemDisplay.getTransformation();
            itemDisplay.setTransformation(new Transformation(
                    actualTransformation.getTranslation(),
                    actualTransformation.getLeftRotation().rotateY((float) Math.toRadians(180)),
                    actualTransformation.getScale().add(OFFSET),
                    actualTransformation.getRightRotation()
            ));

            properties.set(Keys.CUSTOM_BLOCK_KEY, (byte) 1);
            Krimson.customBlocks.registerBlock(customBlock);

            return true;
        };
    }

    /**
     * Retrieve a custom block from the item display<br><br>
     * To get the custom block from the item display, you can use {@link CustomBlockUtils#getCustomBlockFromEntity(Entity)}
     */
    public CustomBlock(ItemDisplay itemDisplay) {
        // Thanks to java not having before super statements (but available in java 22+), we have to do this... horrible thing
        this(
                NamespacedKey.fromString(new PropertiesStore(itemDisplay).get(Keys.DROP_IDENTIFIER_KEY, PersistentDataType.STRING).orElseThrow()),
                Material.valueOf(new PropertiesStore(itemDisplay).get(Keys.BLOCK_INSIDE_KEY, PersistentDataType.STRING).orElseThrow()),
                Codecs.ITEM_STACK_CODEC.decode(new PropertiesStore(itemDisplay).get(Keys.DISPLAYED_ITEM_KEY, PersistentDataType.BYTE_ARRAY).orElseThrow())
        );
        setDisplayAndProperties(itemDisplay);
    }

    protected void setDisplayAndProperties(ItemDisplay itemDisplay) {
        this.spawnedDisplay = itemDisplay;
        this.properties = new PropertiesStore(itemDisplay);

        if(this.properties.has(Keys.DROP_IDENTIFIER_KEY)) {
            this.dropIdentifierField = new PropertiesField<>(Keys.DROP_IDENTIFIER_KEY, properties, PersistentDataType.STRING);
        } else {
            this.dropIdentifierField = new PropertiesField<>(Keys.DROP_IDENTIFIER_KEY, dropIdentifier.toString());
            this.properties.set(dropIdentifierField);
        }

        if(this.properties.has(Keys.BLOCK_INSIDE_KEY)) {
            this.blockInsideField = new PropertiesField<>(Keys.BLOCK_INSIDE_KEY, properties, PersistentDataType.STRING);
        } else {
            this.blockInsideField = new PropertiesField<>(Keys.BLOCK_INSIDE_KEY, blockInside.name());
            this.properties.set(blockInsideField);
        }

        if(this.properties.has(Keys.DISPLAYED_ITEM_KEY)) {
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, properties, PersistentDataType.BYTE_ARRAY);
        } else {
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, Codecs.ITEM_STACK_CODEC.encode(displayedItem));
            this.properties.set(displayedItemField);
        }
    }

    /**
     * Spawn the custom block at the given location
     * @param blockLoc The location of the block
     */
    public void spawn(Location blockLoc) {
        if(blockLoc.getWorld() == null) {
            return;
        }

        blockLoc.setPitch(0);
        blockLoc.setYaw(0);

        blockLoc.getBlock().setType(blockInside);

        PersistentDataContainer pdc = new CustomBlockData(blockLoc.getBlock(), Krimson.getInstance());
        PropertiesStore blockProperties = new PropertiesStore(pdc);

        blockProperties.set(Keys.CUSTOM_BLOCK_KEY, (byte) 1);
        blockProperties.set(Keys.IDENTIFIER_KEY, dropIdentifier.toString());

        if(displayedItem.getType() == Material.PLAYER_HEAD)
        {
            // HEAD
            blockLoc.getWorld().spawn(blockLoc.add(.5, 0 + OFFSET.y(), .5), ItemDisplay.class, itemDisplay -> {
                setDisplayAndProperties(itemDisplay);

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
                setDisplayAndProperties(itemDisplay);

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
    }

    public void tick() {
        ItemDisplay itemDisplay = this.spawnedDisplay;

        if(itemDisplay == null) {
            Krimson.customBlocks.removeBlock(this);
            return;
        }

        Block block = CustomBlockUtils.getBlockFromDisplay(itemDisplay);
        if (block.getType() != this.blockInside) {
            Krimson.getScheduler().runTask(() -> {
                CustomBlockUtils.handleBlockSuppression(block, null);
            });
        }

        // TODO : if you want more accurate per-face lighting, spawn 6 block displays, one for each face, and use the scale transform to flatten them so they're 2d planes; and then do the light get's per-face and apply them to that face only https://discord.com/channels/690411863766466590/741875863271899136/1396952975494217933
        if(Krimson.getConfiguration().getBoolean("preciseLightning", true)) {
            // Precise lightning: check the light level of the block in all cartesian directions
            byte skyLight = BlockUtils.computeLight(Block::getLightFromSky, block);
            byte blockLight = BlockUtils.computeLight(Block::getLightFromBlocks, block);

            this.spawnedDisplay.setBrightness(new Display.Brightness(blockLight, skyLight));
        } else
        {
            // Normal lightning : check the light level of the block above the item
            Block up = block.getRelative(BlockFace.UP);
            this.spawnedDisplay.setBrightness(new Display.Brightness(up.getLightFromBlocks(), up.getLightFromSky()));
        }
    }

    public Location getPosition() {
        return spawnedDisplay.getLocation().add(0, -0.5, 0);
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
        onBreak(event, event.getPlayer());
    }

    /**
     * Called when the custom block is broken. (called also when a player breaks the block)
     */
    public void onBreak(@Nullable Event event, @Nullable Player player) {
        if(player != null) {
            if(player.getGameMode() == GameMode.CREATIVE) return;
        }

        if(dropIdentifier.equals(NamespacedKeyUtils.none())) return;

        Block block = CustomBlockUtils.getBlockFromDisplay(spawnedDisplay);

        Optional<BlockItem> dropItem = Items.REGISTRY.get(dropIdentifier);

        if(dropItem.isEmpty()) {
            Krimson.getInstance().getLogger().warning("Custom block " + dropIdentifier + " has no corresponding item in the registry!");
            return;
        }

        ItemStack itemStack = dropItem.get().getItemStack();

        block.getWorld().dropItemNaturally(block.getLocation().add(.5, .5, .5), itemStack);
    }

    /**
     * Called when the custom block is unloaded (e.g. when the chunk is unloaded)
     */
    public void onUnload() {
    }
}
