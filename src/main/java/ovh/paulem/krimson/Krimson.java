package ovh.paulem.krimson;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import lombok.Getter;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.PluginManager;
import ovh.paulem.krimson.blocks.CustomBlock;
import ovh.paulem.krimson.blocks.CustomBlockTypeChecker;
import ovh.paulem.krimson.blocks.list.CustomBlocksList;
import ovh.paulem.krimson.commands.CommandDisplay;
import ovh.paulem.krimson.common.KrimsonPlugin;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.listeners.CustomBlockActionListener;
import ovh.paulem.krimson.listeners.CustomBlockSuppressionListener;
import ovh.paulem.krimson.listeners.LightSourcePreventionListener;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ovh.paulem.krimson.properties.PropertiesStore;

import java.util.*;

// TODO : Add place custom block from inventory
// TODO : Seems like PDC is slow https://github.com/PaperMC/Paper/pull/3359
/**
 * Main class for the BountifulLib plugin.
 * Handles plugin initialization, event registration, and custom block management.
 */
public final class Krimson extends KrimsonPlugin implements Listener {
    @Getter
    private static TaskScheduler scheduler;
    @Getter
    private static FileConfiguration configuration;

    public static CustomBlocksList<CustomBlock> customBlocks = new CustomBlocksList<>();

    public static Set<Chunk> processedChunks = new HashSet<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        saveDefaultConfig();
        configuration = getConfig();

        scheduler = UniversalScheduler.getScheduler(this);

        // Events
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(this, this);
        pluginManager.registerEvents(new CustomBlockSuppressionListener(), this);
        pluginManager.registerEvents(new CustomBlockActionListener(), this);
        pluginManager.registerEvents(new LightSourcePreventionListener(), this);

        // Main
        getLogger().info("Hello from Krimson API!");

        getScheduler().runTaskTimerAsynchronously(() -> {
            for (CustomBlock customBlock : customBlocks) {
                if(customBlock.getSpawnedDisplay().isValid()) customBlock.tick();
            }
        }, 1L, 1L);
        getLogger().info("Scheduled ticking !");

        // Commands
        getCommand("display").setExecutor(new CommandDisplay());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
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

        LinkedList<CustomBlock> gotCustomBlocks = new LinkedList<>();

        Collection<ItemDisplay> itemDisplays = Arrays.stream(chunk.getEntities()).filter(Krimson::isCustomBlock).map(entity -> (ItemDisplay) entity).toList();
        if(!itemDisplays.isEmpty())
        {
            for (ItemDisplay itemDisplay : itemDisplays) {
                CustomBlock e1 = new CustomBlockTypeChecker(itemDisplay).get();
                gotCustomBlocks.add(e1);
            }
            customBlocks.addAll(gotCustomBlocks);
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
}