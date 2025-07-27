package ovh.paulem.krimson;

import com.google.common.collect.Iterables;
import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.blocks.CustomBlock;
import ovh.paulem.krimson.blocks.CustomBlockTypeChecker;
import ovh.paulem.krimson.blocks.list.CustomBlocks;
import ovh.paulem.krimson.commands.CommandDisplay;
import ovh.paulem.krimson.commands.CommandKrimson;
import ovh.paulem.krimson.common.KrimsonPlugin;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.items.BlockItem;
import ovh.paulem.krimson.items.Items;
import ovh.paulem.krimson.listeners.*;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ovh.paulem.krimson.properties.PropertiesStore;
import ovh.paulem.krimson.resourcepack.ResourcePackHosting;
import ovh.paulem.krimson.resourcepack.creator.ResourcePackKt;
import ovh.paulem.krimson.utils.CustomBlockUtils;

import java.util.*;

// TODO : Add place custom block from inventory
// TODO : Seems like PDC is slow https://github.com/PaperMC/Paper/pull/3359
/**
 * Main class for the BountifulLib plugin.
 * Handles plugin initialization, event registration, and custom block management.
 */
public final class Krimson extends KrimsonPlugin<Krimson> implements Listener {
    public static ResourcePackHosting packHosting;

    public static CustomBlocks<CustomBlock> customBlocks = new CustomBlocks<>();

    public static Set<Chunk> processedChunks = new HashSet<>();

    @Override
    public void onEnable() {
        super.onEnable();

        // Events
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(this, this);
        pluginManager.registerEvents(new CustomBlockSuppressionListener(), this);
        pluginManager.registerEvents(new CustomBlockActionListener(), this);
        pluginManager.registerEvents(new LightSourcePreventionListener(), this);
        pluginManager.registerEvents(new BlockItemHandlerListener(), this);
        pluginManager.registerEvents(new MigrationListener(), this);
        CustomBlockData.registerListener(this);

        // Main
        getLogger().info("Hello from Krimson API!");

        getScheduler().runTaskTimerAsynchronously(() -> {
            //long startTime = System.nanoTime();

            customBlocks.parallelStream()
                .filter(customBlock -> customBlock.getSpawnedDisplay().isValid())
                .forEach(CustomBlock::tick);

            //long endTime = System.nanoTime();

            //getLogger().info("CustomBlocks tick iteration took " + (endTime - startTime) / 1_000_000 + " ms");
        }, 1L, 1L);

        getLogger().info("Scheduled ticking!");

        // Commands
        getCommand("display").setExecutor(new CommandDisplay());
        getCommand("krimson").setExecutor(new CommandKrimson());
        getLogger().info("Registered commands!");

        Items.init();

        packHosting = new ResourcePackHosting(ResourcePackKt.main(getDataFolder()));
        pluginManager.registerEvents(packHosting, this);
        packHosting.start();

        getLogger().info("Finished loading Krimson API!");
    }

    @Override
    public void onDisable() {
        super.onDisable();

        packHosting.stop();

        getLogger().info("Goodbye from Krimson API!");
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        Entity entity = event.getEntity();
        if (isCustomBlock(entity)) {
            CustomBlockSuppressionListener.onCustomBlockDeath((ItemDisplay) entity, true);
        }
    }

    /**
     * Event handler for world load events.
     * Adds existing custom blocks in the loaded world to the customBlocks list.
     *
     * @param event the WorldLoadEvent
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        if (processedChunks.contains(chunk)) {
            return;
        }

        Collection<ItemDisplay> itemDisplays = Arrays.stream(chunk.getEntities()).filter(Krimson::isCustomBlock).map(entity -> (ItemDisplay) entity).toList();
        if(!itemDisplays.isEmpty())
        {
            for (ItemDisplay itemDisplay : itemDisplays) {
                CustomBlock customBlock = new CustomBlockTypeChecker(itemDisplay).get();
                customBlocks.add(customBlock);
            }
        }

        processedChunks.add(chunk);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        if (!processedChunks.contains(chunk)) {
            return;
        }

        customBlocks.removeIf(customBlock -> {
            ItemDisplay display = customBlock.getSpawnedDisplay();

            if (display.getLocation().getChunk().equals(chunk)) {
                getLogger().info("Unloading custom block " + customBlock.getBlockInside() + " at " + display.getLocation() + "!");

                customBlock.onUnload();
                return true;
            }

            return false;
        });

        processedChunks.remove(chunk);
    }

    /**
     * Checks if the given entity is a custom block.
     *
     * @param entity the entity to check
     * @return true if the entity is a custom block, false otherwise
     */
    public static boolean isCustomBlock(Entity entity) {
        return entity.isValid() && entity instanceof ItemDisplay itemDisplay && isCustomBlock(itemDisplay);
    }

    /**
     * Checks if the given item display is a custom block.
     *
     * @param itemDisplay the item display to check
     * @return true if the item display is a custom block, false otherwise
     */
    public static boolean isCustomBlock(ItemDisplay itemDisplay) {
        return itemDisplay.isValid() && new PropertiesStore(itemDisplay).has(Keys.CUSTOM_BLOCK_KEY);
    }

    /**
     * Checks if the given block is a custom block.
     */
    public static boolean isCustomBlock(Block block) {
        PersistentDataContainer pdc = new CustomBlockData(block, Krimson.getInstance());
        return new PropertiesStore(pdc).has(Keys.CUSTOM_BLOCK_KEY);
    }

    public static void retryCustomBlockFromBlock(Block block, @Nullable Player player) {
        if (isCustomBlock(block)) {
            @Nullable Entity display = CustomBlockUtils.getDisplayFromBlock(block);

            if(display == null) {
                PersistentDataContainer pdc = new CustomBlockData(block, Krimson.getInstance());
                @Nullable String identifier = new PropertiesStore(pdc).get(Keys.IDENTIFIER_KEY, PersistentDataType.STRING).orElse(null);

                if(identifier == null) {
                    return;
                }

                @Nullable NamespacedKey key = NamespacedKey.fromString(identifier);
                if (key == null) {
                    getInstance().getLogger().warning("Invalid NamespacedKey for custom block at " + block.getLocation() + ": " + identifier);
                    return;
                }

                @Nullable BlockItem blockItem = Items.REGISTRY.getOrNull(key);

                if(blockItem == null) {
                    getInstance().getLogger().warning("No BlockItem found for custom block at " + block.getLocation() + ": " + identifier);
                    return;
                }

                blockItem.getAction().accept(blockItem, player, block.getLocation());
            }
        }
    }
}