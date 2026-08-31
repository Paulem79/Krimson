package net.paulem.krimson.blocks.custom;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import net.paulem.krimson.KrimsonPlugin;
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
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.blocks.Blocks;
import net.paulem.krimson.blocks.mining.MiningProperties;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.items.CustomItem;
import net.paulem.krimson.items.Items;
import net.paulem.krimson.properties.PDCWrapper;
import net.paulem.krimson.registry.RegistryKey;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CustomBlock implements RegistryKey<NamespacedKey> {
    public static final Vector3f OFFSET = new Vector3f(.0005f);
    private static final String REGISTRY_REFERENCE_ERROR_MESSAGE = "You must clone this registry instance of the custom block before editing it.";

    @Getter
    protected boolean registryReference; // This is used to check if this instance is a registry instance, so that we need to clone it before using it

    @Getter
    private final NamespacedKey key;
    @Getter
    protected final NamespacedKey dropIdentifier;
    @Getter
    protected final Material blockMaterial;
    @Getter
    protected ItemDisplay linkedDisplay;

    /**
     * Displays used when per face lighting is enabled: one flattened display per cartesian face, each lit
     * by the block laid against it. Empty when the block is rendered by a single display, in which case
     * {@link #linkedDisplay} is the one to use (and it is {@code null} the other way around).
     *
     * <p>Concurrent because it is written when the block spawns, on the main thread, and read by
     * {@link #tickLight()} on the async ticking thread.</p>
     */
    @Getter
    protected final Map<BlockFace, ItemDisplay> faceDisplays = new ConcurrentHashMap<>();

    /**
     * Last brightness pushed to each display, packed as {@code blockLight << 4 | skyLight} and keyed by
     * face ({@link BlockFace#SELF} for the single display). Lighting is ticked every tick for every loaded
     * block, so we only send an update when the computed value actually changed.
     */
    private final Map<BlockFace, Integer> lastBrightness = new ConcurrentHashMap<>();
    @Getter
    protected Block block;
    @Getter
    protected CustomBlockProperties properties;
    /**
     * How this block reacts to mining. Defaults to {@link MiningProperties#INHERIT}, meaning the block keeps
     * the vanilla behaviour of the material carrying it.
     */
    @Getter
    protected MiningProperties miningProperties = MiningProperties.INHERIT;

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
        this(
                NamespacedKey.fromString(new PDCWrapper(block).get(Keys.IDENTIFIER).orElseThrow()),
                NamespacedKey.fromString(new PDCWrapper(block).get(Keys.DROP_IDENTIFIER).orElseThrow()),
                Material.valueOf(new PDCWrapper(block).get(Keys.BLOCK_INSIDE).orElseThrow())
        );
        this.registryReference = false;
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
        copy.miningProperties = this.miningProperties;

        return copy;
    }

    /**
     * Sets how this block reacts to mining. Only allowed on a registry template, so it must be called before
     * {@code Blocks.REGISTRY.freeze()}; live copies inherit the value through {@link #copyOf()}.
     */
    public void setMiningProperties(MiningProperties miningProperties) {
        Preconditions.checkState(isRegistryReference(), "Mining properties must be set on the registry template, before freezing the registry.");

        this.miningProperties = miningProperties;
    }

    /**
     * Resolves the mining properties to actually use for this block.
     *
     * <p>Instances reconstructed from the world (see {@link #CustomBlock(Block)} and
     * {@link CustomBlockTypeChecker}) carry no template data, so the registry stays the source of truth —
     * same approach as the drop identifier.</p>
     */
    public MiningProperties resolveMiningProperties() {
        if (!miningProperties.inherits()) {
            return miningProperties;
        }

        return Blocks.REGISTRY.get(key)
                .map(CustomBlock::getMiningProperties)
                .orElse(MiningProperties.INHERIT);
    }

    protected void setDisplayAndProperties(Block block) {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        this.block = block;
        this.properties = createProperties(block);
    }

    protected CustomBlockProperties createProperties(Block block) {
        return new CustomBlockProperties(block, this);
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

        this.linkedDisplay = null;
        faceDisplays.clear();
        lastBrightness.clear();

        if (getItemDisplayStack().getType() == Material.PLAYER_HEAD) {
            // HEAD
            Location spawnLoc = blockLoc.add(.5, 0 + OFFSET.y(), .5);

            removeGhostDisplays(spawnLoc);

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
        } else if (usesPerFaceLighting()) {
            // BLOCK, one flat display per face
            spawnFaceDisplays(blockLoc.add(.5, .5, .5));
        } else {
            // BLOCK
            Location spawnLoc = blockLoc.add(.5, .5, .5);

            removeGhostDisplays(spawnLoc);

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

        registerLive(block);
    }

    /**
     * Whether this block is rendered as six flattened displays, one per face, each lit on its own.
     * Player head models are not full cubes, so they always keep a single display.
     */
    protected boolean usesPerFaceLighting() {
        return KrimsonPlugin.getConfiguration().isPreciseLightning()
                && getItemDisplayStack().getType() != Material.PLAYER_HEAD;
    }

    /**
     * Removes the displays already sitting at the given location, to prevent ghosts.
     */
    protected void removeGhostDisplays(Location spawnLoc) {
        spawnLoc.getWorld().getNearbyEntities(spawnLoc, 0.2, 0.2, 0.2).stream()
                .filter(ItemDisplay.class::isInstance)
                .forEach(org.bukkit.entity.Entity::remove);
    }

    /**
     * Spawns one display per cartesian face, each flattened into a 2D plane laid on that face, so that
     * {@link #tickLight()} can light every face with its own neighbour.
     *
     * <p>All six displays are spawned at the center of the block and only differ by their transformation,
     * so the ghost cleanup and the respawn logic stay the same whatever the lighting mode is - which is
     * also what makes toggling the option at runtime self healing.</p>
     */
    protected void spawnFaceDisplays(Location spawnLoc) {
        removeGhostDisplays(spawnLoc);

        ItemStack stack = getItemDisplayStack();
        Map<BlockFace, ItemDisplay> displays = new EnumMap<>(BlockFace.class);

        for (BlockFace face : BlockFace.values()) {
            if (!face.isCartesian()) {
                continue;
            }

            displays.put(face, spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, itemDisplay -> {
                itemDisplay.setPersistent(false);
                itemDisplay.setItemStack(stack);

                itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);

                itemDisplay.setTransformation(faceTransformation(face));
            }));
        }

        faceDisplays.clear();
        faceDisplays.putAll(displays);

        tickLight();
    }

    /**
     * Builds the transformation flattening the block model into the plane of the given face, keeping the
     * orientation used by the single display so the rendering is unchanged.
     */
    protected Transformation faceTransformation(BlockFace face) {
        float thickness = OFFSET.x();
        // Pushed slightly outside the block it covers, to avoid z-fighting with the real block
        float distance = .5f + thickness;

        Vector3f translation = new Vector3f(face.getModX(), face.getModY(), face.getModZ()).mul(distance);
        Vector3f scale = new Vector3f(
                face.getModX() != 0 ? thickness : 1f,
                face.getModY() != 0 ? thickness : 1f,
                face.getModZ() != 0 ? thickness : 1f
        );

        return new Transformation(
                translation,
                new Quaternionf().rotateY((float) Math.toRadians(180)),
                scale,
                new Quaternionf()
        );
    }

    /**
     * Marks the block as a live custom block: builds its properties, writes the custom block marker to the
     * PDC and hands the instance to the tracker. Shared by every backend (display based or not).
     */
    protected void registerLive(Block block) {
        setDisplayAndProperties(block);
        properties.getContainer().set(Keys.CUSTOM_BLOCK, (byte) 1);
        KrimsonAPI.customBlocks.registerBlock(this);
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

        if (!hasValidDisplays()) {
            spawnDisplay(block.getLocation());
        }
    }

    /**
     * Whether the displays expected for the current lighting mode are all alive. Toggling
     * {@code preciseLightning} at runtime makes this false, so the block respawns with the right layout.
     */
    protected boolean hasValidDisplays() {
        if (usesPerFaceLighting()) {
            return faceDisplays.size() == 6 && faceDisplays.values().stream().allMatch(ItemDisplay::isValid);
        }

        return linkedDisplay != null && linkedDisplay.isValid();
    }

    public final void tickLight() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        // Precise lightning: the block is rendered as 6 flat displays (see spawnFaceDisplays), so each of
        // them takes the light of the block laid against its face, like vanilla does for a real block.
        // Reference: https://discord.com/channels/690411863766466590/741875863271899136/1396952975494217933
        if (!faceDisplays.isEmpty()) {
            faceDisplays.forEach((face, display) -> {
                Block neighbour = block.getRelative(face);

                applyBrightness(face, display, neighbour.getLightFromBlocks(), neighbour.getLightFromSky());
            });

            return;
        }

        // Normal lightning: a single display, hence a single brightness for the whole block, taken as the
        // brightest of the cartesian neighbours
        byte skyLight = BlockUtils.computeLight(Block::getLightFromSky, block);
        byte blockLight = BlockUtils.computeLight(Block::getLightFromBlocks, block);

        applyBrightness(BlockFace.SELF, linkedDisplay, blockLight, skyLight);
    }

    /**
     * Applies a brightness to a display, skipping the update when it did not change since the last tick.
     */
    private void applyBrightness(BlockFace face, @Nullable ItemDisplay display, int blockLight, int skyLight) {
        if (display == null || !display.isValid()) {
            return; // tickSync respawns it
        }

        int packed = blockLight << 4 | skyLight;
        Integer previous = lastBrightness.get(face);
        if (previous != null && previous == packed) {
            return;
        }

        display.setBrightness(new Display.Brightness(blockLight, skyLight));
        lastBrightness.put(face, packed);
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
            // Utilisation directe des propriétés du record DataKey pour le PDC natif de Bukkit
            pdc.set(Keys.IDENTIFIER.key(), Keys.IDENTIFIER.type(), key.toString());

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
        Player player = event.getPlayer();
        onBreak(event, player);

        // Damage player tool
        player.damageItemStack(player.getInventory().getItemInMainHand(), 1);
    }

    /**
     * Called when the custom block is broken. (called also when a player breaks the block)
     */
    public void onBreak(@Nullable Event event, @Nullable Player player) {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        remove();

        if (player != null && player.getGameMode() == GameMode.CREATIVE) return;

        // The mining system marks a break made with a tool that cannot harvest the block: it still breaks,
        // but yields nothing, exactly like a vanilla ore mined with a bare hand.
        if (event instanceof BlockBreakEvent blockBreakEvent && !blockBreakEvent.isDropItems()) return;

        // DROP ITEM PART
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


        if (linkedDisplay != null) {
            linkedDisplay.remove();
            linkedDisplay = null;
        }

        faceDisplays.values().forEach(org.bukkit.entity.Entity::remove);
        faceDisplays.clear();
        lastBrightness.clear();
    }

    public void remove() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        onUnload();

        block.setType(Material.AIR);

        getProperties().getContainer().getContainer().clear();

        Block pdcBlock = getProperties().getContainer().getContainer().getBlock();
        if (pdcBlock != null) {
            pdcBlock.getChunk().getPersistentDataContainer().remove(PersistentDataUtils.getKey(KrimsonPlugin.getInstance(), pdcBlock));
        }

        KrimsonAPI.customBlocks.removeBlock(this);
    }
}
