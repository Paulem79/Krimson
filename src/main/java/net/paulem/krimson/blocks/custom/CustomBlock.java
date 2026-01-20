package net.paulem.krimson.blocks.custom;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimson.utils.*;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.codec.Codecs;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.items.CustomItem;
import net.paulem.krimson.items.Items;
import net.paulem.krimson.properties.PDCWrapper;
import net.paulem.krimson.properties.PropertiesField;
import net.paulem.krimson.registry.RegistryKey;

import java.util.Optional;
import java.util.function.Consumer;

public class CustomBlock implements RegistryKey<NamespacedKey> {
    public static final Vector3f OFFSET = new Vector3f(.0005f);
    private static final String REGISTRY_REFERENCE_ERROR_MESSAGE = "You must clone this registry instance of the custom block before editing it.";

    @Getter
    protected boolean registryReference; // This is used to check if this instance is a registry instance, so that we need to clone it before using it

    private final NamespacedKey key;
    @Getter
    protected final NamespacedKey dropIdentifier;
    @Getter
    protected final Material blockMaterial;
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
     */
    public CustomBlock(NamespacedKey key, NamespacedKey dropIdentifier, Material blockMaterial) {
        Preconditions.checkArgument(blockMaterial.isBlock(), "The material inside must be a block!");
        this.key = key;
        this.dropIdentifier = dropIdentifier;
        this.blockMaterial = blockMaterial;

        this.registryReference = true; // This is a registry reference, so we need to clone it before using it
    }

    /**
     * Retrieve a custom block from the item display
     */
    public CustomBlock(Block block) {
        // Thanks to java not having before super statements (but available in java 22+), we have to do this... horrible thing
        this(
                // Get the key from the block's persistent data container
                NamespacedKey.fromString(new PDCWrapper(block).get(Keys.IDENTIFIER_KEY, PersistentDataType.STRING).orElseThrow()),
                // Get drop item
                NamespacedKey.fromString(new PDCWrapper(block).get(Keys.DROP_IDENTIFIER_KEY, PersistentDataType.STRING).orElseThrow()),
                // Get block material
                Material.valueOf(new PDCWrapper(block).get(Keys.BLOCK_INSIDE_KEY, PersistentDataType.STRING).orElseThrow())
        );

        this.registryReference = false; // This is not a registry reference, so we can use it directly

        spawnDisplay(block.getLocation());

        setDisplayAndProperties(block);
    }

    /**
     * Creates a live instance from this registry template.
     * This replaces the clone() implementation.
     */
    public CustomBlock copyOf() {
        // 1. Create a new instance using this object's data
        CustomBlock copy = new CustomBlock(this.key, this.dropIdentifier, this.blockMaterial);

        // 2. Configure the specific state for a "live" block
        copy.registryReference = false;
        copy.meta = this.meta;

        return copy;
    }

    protected void setDisplayAndProperties(Block block) {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        this.block = block;
        this.properties = new PDCWrapper(block);

        this.properties.set(Keys.IDENTIFIER_KEY, key.toString());

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
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, Codecs.ITEM_STACK_CODEC.encode(getItemDisplayStack()));
            this.properties.set(displayedItemField);
        }
    }

    /**
     * Spawn the custom block at the given location
     *
     * @param blockLoc The location of the block
     */
    public void spawn(Location blockLoc) {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        if (blockLoc.getWorld() == null) {
            return;
        }

        blockLoc.getBlock().setType(blockMaterial);

        spawnDisplay(blockLoc);
    }

    public void spawnDisplay(Location blockLoc) {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        blockLoc = blockLoc.clone();

        if (blockLoc.getWorld() == null) {
            return;
        }

        this.block = blockLoc.getBlock();

        blockLoc.setPitch(0);
        blockLoc.setYaw(0);

        if (getItemDisplayStack().getType() == Material.PLAYER_HEAD) {
            // HEAD
            Location spawnLoc = blockLoc.add(.5, 0 + OFFSET.y(), .5);

            // Remove existing displays to prevent ghosts
            spawnLoc.getWorld().getNearbyEntities(spawnLoc, 0.2, 0.2, 0.2).stream()
                    .filter(e -> e instanceof ItemDisplay)
                    .forEach(org.bukkit.entity.Entity::remove);

            blockLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, itemDisplay -> {
                this.linkedDisplay = itemDisplay;
                linkedDisplay.setPersistent(false);
                linkedDisplay.setItemStack(getItemDisplayStack());

                linkedDisplay.setTransformation(new Transformation(
                        new Vector3f(0f, 1f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(2f).add(OFFSET.mul(2)),
                        new AxisAngle4f(0f, 0f, 0f, 1f)
                ));

                Transformation actualTransformation = linkedDisplay.getTransformation();
                linkedDisplay.setTransformation(new Transformation(
                        actualTransformation.getTranslation(),
                        actualTransformation.getLeftRotation().rotateY((float) Math.toRadians(180)),
                        actualTransformation.getScale().add(OFFSET),
                        actualTransformation.getRightRotation()
                ));

                tickLight();
            });
        } else {
            // BLOCK
            Location spawnLoc = blockLoc.add(.5, .5, .5);

            // Remove existing displays to prevent ghosts
            spawnLoc.getWorld().getNearbyEntities(spawnLoc, 0.2, 0.2, 0.2).stream()
                    .filter(e -> e instanceof ItemDisplay)
                    .forEach(org.bukkit.entity.Entity::remove);

            linkedDisplay = blockLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, itemDisplay -> {
                this.linkedDisplay = itemDisplay;
                linkedDisplay.setPersistent(false);
                linkedDisplay.setItemStack(getItemDisplayStack());

                linkedDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);

                linkedDisplay.setTransformation(new Transformation(
                        new Vector3f(0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(1f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)
                ));

                Transformation actualTransformation = linkedDisplay.getTransformation();
                linkedDisplay.setTransformation(new Transformation(
                        actualTransformation.getTranslation(),
                        actualTransformation.getLeftRotation().rotateY((float) Math.toRadians(180)),
                        actualTransformation.getScale().add(OFFSET),
                        actualTransformation.getRightRotation()
                ));

                tickLight();
            });
        }

        setDisplayAndProperties(block);
        properties.set(Keys.CUSTOM_BLOCK_KEY, (byte) 1);
        Krimson.customBlocks.registerBlock(this);
    }

    public void tickAsync() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        if (block.getType() != this.blockMaterial) {
            KrimsonPlugin.getScheduler().runTask(() ->
                CustomBlockUtils.handleBlockSuppression(block, null)
            );
        }

        tickLight();
    }

    public void tickSync() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        if (linkedDisplay == null || !linkedDisplay.isValid()) {
            spawnDisplay(block.getLocation());
        }
    }

    public final void tickLight() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        // TODO : if you want more accurate per-face lighting, spawn 6 block displays, one for each face, and use the scale transform to flatten them so they're 2d planes; and then do the light get's per-face and apply them to that face only https://discord.com/channels/690411863766466590/741875863271899136/1396952975494217933
        if (KrimsonPlugin.getConfiguration().getBoolean("preciseLightning", true)) {
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

    // Registry of item meta to get the reference from OR better get from original block reference
    @Setter
    @Getter(lombok.AccessLevel.PROTECTED)
    @Nullable
    private Consumer<ItemMeta> meta;

    public ItemStack getItemDisplayStack() {
        ItemStack stack = ItemUtils.getWithItemModel(new ItemStack(getBlockMaterial()), key);
        ItemMeta stackItemMeta = stack.getItemMeta();
        if (stackItemMeta != null) {
            PersistentDataContainer pdc = stackItemMeta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(KrimsonPlugin.getInstance(), Keys.IDENTIFIER_KEY), PersistentDataType.STRING, key.toString());

            if(this.meta != null) this.meta.accept(stackItemMeta);

            stack.setItemMeta(stackItemMeta);
        }

        return stack;
    }

    public Location getPosition() {
        return block.getLocation();
    }

    /**
     * Called when a player interacts with the custom block
     */
    public void onInteract(PlayerInteractEvent event) {
        event.getPlayer().sendMessage("You interacted with a custom block: " + key.toString());
        event.getPlayer().sendMessage("Class type: " + this.getClass().getSimpleName());
    }

    /**
     * Called when the custom block is placed by a player.
     */
    public void onPlace(BlockPlaceEvent event) {
        // Default implementation does nothing
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
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        remove();

        if (player != null && player.getGameMode() == GameMode.CREATIVE) return;

        // DROP ITEM PART
        System.out.println(dropIdentifier);
        if (dropIdentifier.equals(NamespacedKeyUtils.none())) return;

        Optional<CustomItem> dropItem = Items.REGISTRY.get(dropIdentifier);

        if (dropItem.isEmpty()) {
            KrimsonPlugin.getInstance().getLogger().warning("Custom block " + dropIdentifier + " has no corresponding item in the registry!");
            return;
        }

        ItemStack itemStack = dropItem.get().getItemStack();
        block.getWorld().dropItemNaturally(block.getLocation().add(.5, .5, .5), itemStack);
    }

    /**
     * Called when the custom block is unloaded (e.g. when the chunk is unloaded)
     */
    public void onUnload() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        KrimsonPlugin.getInstance().getLogger().info("Unloading custom block " + dropIdentifier + " at " + block.getLocation());


        linkedDisplay.remove();
    }

    public void remove() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        onUnload();

        block.setType(Material.AIR);

        getProperties().getContainer().clear();

        Block pdcBlock = getProperties().getContainer().getBlock();
        if (pdcBlock != null) {
            pdcBlock.getChunk().getPersistentDataContainer().remove(PersistentDataUtils.getKey(KrimsonPlugin.getInstance(), pdcBlock));
        }

        Krimson.customBlocks.removeBlock(this);
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }
}
