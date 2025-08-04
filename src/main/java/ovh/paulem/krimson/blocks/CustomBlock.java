package ovh.paulem.krimson.blocks;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.codec.Codecs;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.items.BlockItem;
import ovh.paulem.krimson.items.Items;
import ovh.paulem.krimson.properties.PDCWrapper;
import ovh.paulem.krimson.properties.PropertiesField;
import ovh.paulem.krimson.utils.BlockUtils;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import ovh.paulem.krimson.utils.NamespacedKeyUtils;
import ovh.paulem.krimson.utils.PersistentDataUtils;

import java.util.Optional;

public class CustomBlock {
    public static final Vector3f OFFSET = new Vector3f(.0005f);

    @Getter
    protected final NamespacedKey dropIdentifier;
    @Getter
    protected final Material blockMaterial;
    @Getter
    protected final ItemStack displayedItem;
    private final Runnable commonArguments;
    @Getter
    protected ItemDisplay linkedDisplay;
    @Getter
    protected Block block;
    @Getter
    protected PDCWrapper properties;
    @Getter
    private PropertiesField<String> dropIdentifierField;
    @Getter
    private PropertiesField<String> blockMaterialField;
    @Getter
    @Nullable
    private PropertiesField<byte[]> displayedItemField;

    /**
     * Create a custom block with the given item
     *
     * @param displayedItem The item to display
     */
    public CustomBlock(NamespacedKey dropIdentifier, Material blockMaterial, ItemStack displayedItem) {
        Preconditions.checkArgument(blockMaterial.isBlock(), "The material inside must be a block!");
        this.dropIdentifier = dropIdentifier;
        this.blockMaterial = blockMaterial;
        this.displayedItem = displayedItem;

        commonArguments = () -> {
            linkedDisplay.setItemStack(displayedItem);

            setDisplayAndProperties(block);

            Transformation actualTransformation = linkedDisplay.getTransformation();
            linkedDisplay.setTransformation(new Transformation(
                    actualTransformation.getTranslation(),
                    actualTransformation.getLeftRotation().rotateY((float) Math.toRadians(180)),
                    actualTransformation.getScale().add(OFFSET),
                    actualTransformation.getRightRotation()
            ));

            tickLight();

            properties.set(Keys.CUSTOM_BLOCK_KEY, (byte) 1);
            Krimson.customBlocks.registerBlock(this);
        };
    }

    /**
     * Retrieve a custom block from the item display
     */
    public CustomBlock(Block block) {
        // Thanks to java not having before super statements (but available in java 22+), we have to do this... horrible thing
        this(
                NamespacedKey.fromString(new PDCWrapper(block).get(Keys.DROP_IDENTIFIER_KEY, PersistentDataType.STRING).orElseThrow()),
                Material.valueOf(new PDCWrapper(block).get(Keys.BLOCK_INSIDE_KEY, PersistentDataType.STRING).orElseThrow()),
                Codecs.ITEM_STACK_CODEC.decode(new PDCWrapper(block).get(Keys.DISPLAYED_ITEM_KEY, PersistentDataType.BYTE_ARRAY).orElseThrow())
        );

        spawnDisplay(block.getLocation());

        setDisplayAndProperties(block);
    }

    protected void setDisplayAndProperties(Block block) {
        this.block = block;
        this.properties = new PDCWrapper(block);

        this.properties.set(Keys.IDENTIFIER_KEY, dropIdentifier.toString());

        if (this.properties.has(Keys.DROP_IDENTIFIER_KEY)) {
            this.dropIdentifierField = new PropertiesField<>(Keys.DROP_IDENTIFIER_KEY, properties, PersistentDataType.STRING);
        } else {
            this.dropIdentifierField = new PropertiesField<>(Keys.DROP_IDENTIFIER_KEY, dropIdentifier.toString());
            this.properties.set(dropIdentifierField);
        }

        if (this.properties.has(Keys.BLOCK_INSIDE_KEY)) {
            this.blockMaterialField = new PropertiesField<>(Keys.BLOCK_INSIDE_KEY, properties, PersistentDataType.STRING);
        } else {
            this.blockMaterialField = new PropertiesField<>(Keys.BLOCK_INSIDE_KEY, blockMaterial.name());
            this.properties.set(blockMaterialField);
        }

        if (this.properties.has(Keys.DISPLAYED_ITEM_KEY)) {
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, properties, PersistentDataType.BYTE_ARRAY);
        } else {
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, Codecs.ITEM_STACK_CODEC.encode(displayedItem));
            this.properties.set(displayedItemField);
        }
    }

    /**
     * Spawn the custom block at the given location
     *
     * @param blockLoc The location of the block
     */
    public void spawn(Location blockLoc) {
        if (blockLoc.getWorld() == null) {
            return;
        }

        blockLoc.getBlock().setType(blockMaterial);

        spawnDisplay(blockLoc);
    }

    public void spawnDisplay(Location blockLoc) {
        if (blockLoc.getWorld() == null) {
            return;
        }

        this.block = blockLoc.getBlock();

        blockLoc.setPitch(0);
        blockLoc.setYaw(0);

        if (displayedItem.getType() == Material.PLAYER_HEAD) {
            // HEAD
            blockLoc.getWorld().spawn(blockLoc.add(.5, 0 + OFFSET.y(), .5), ItemDisplay.class, itemDisplay -> {
                this.linkedDisplay = itemDisplay;

                itemDisplay.setRotation(180F, 0F);

                itemDisplay.setTransformation(new Transformation(
                        new Vector3f(0f, 1f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(2f).add(OFFSET.mul(2)),
                        new AxisAngle4f(0f, 0f, 0f, 1f)
                ));

                commonArguments.run();
            });
        } else {
            // BLOCK
            linkedDisplay = blockLoc.getWorld().spawn(blockLoc.add(.5, .5, .5), ItemDisplay.class, itemDisplay -> {
                this.linkedDisplay = itemDisplay;

                itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);

                itemDisplay.setTransformation(new Transformation(
                        new Vector3f(0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(1f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)
                ));

                commonArguments.run();
            });
        }
    }

    public void tickAsync() {
        if (block.getType() != this.blockMaterial) {
            Krimson.getScheduler().runTask(() -> {
                CustomBlockUtils.handleBlockSuppression(block, null);
            });
        }

        tickLight();
    }

    public void tickSync() {
        if (linkedDisplay == null || !linkedDisplay.isValid()) {
            spawnDisplay(block.getLocation());
        }
    }

    public final void tickLight() {
        // TODO : if you want more accurate per-face lighting, spawn 6 block displays, one for each face, and use the scale transform to flatten them so they're 2d planes; and then do the light get's per-face and apply them to that face only https://discord.com/channels/690411863766466590/741875863271899136/1396952975494217933
        if (Krimson.getConfiguration().getBoolean("preciseLightning", true)) {
            // Precise lightning: check the light level of the block in all cartesian directions
            byte skyLight = BlockUtils.computeLight(Block::getLightFromSky, block);
            byte blockLight = BlockUtils.computeLight(Block::getLightFromBlocks, block);

            linkedDisplay.setBrightness(new Display.Brightness(blockLight, skyLight));
        } else {
            // Normal lightning : check the light level of the block above the item
            Block up = block.getRelative(BlockFace.UP);
            linkedDisplay.setBrightness(new Display.Brightness(up.getLightFromBlocks(), up.getLightFromSky()));
        }
    }

    public Location getPosition() {
        return block.getLocation();
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
        remove();

        if (player != null) {
            if (player.getGameMode() == GameMode.CREATIVE) return;
        }

        // DROP ITEM PART
        if (dropIdentifier.equals(NamespacedKeyUtils.none())) return;

        Optional<BlockItem> dropItem = Items.REGISTRY.get(dropIdentifier);

        if (dropItem.isEmpty()) {
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
        Krimson.getInstance().getLogger().info("Unloading custom block " + dropIdentifier + " at " + block.getLocation());

        linkedDisplay.remove();
    }

    public void remove() {
        onUnload();

        block.setType(Material.AIR);

        getProperties().getContainer().clear();

        Block pdcBlock = getProperties().getContainer().getBlock();
        if (pdcBlock != null) {
            pdcBlock.getChunk().getPersistentDataContainer().remove(PersistentDataUtils.getKey(Krimson.getInstance(), pdcBlock));
        }

        Krimson.customBlocks.removeBlock(this);
    }

}
