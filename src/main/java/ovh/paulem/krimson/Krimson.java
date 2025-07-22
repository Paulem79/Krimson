package ovh.paulem.krimson;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import lombok.Getter;
import org.bukkit.event.world.ChunkLoadEvent;
import ovh.paulem.krimson.blocks.CustomBlock;
import ovh.paulem.krimson.blocks.CustomBlockTypeChecker;
import ovh.paulem.krimson.blocks.list.CustomBlocksList;
import ovh.paulem.krimson.commands.CommandDisplay;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

// TODO : Add place custom block from inventory
// TODO : Seems like PDC is slow https://github.com/PaperMC/Paper/pull/3359
/**
 * Main class for the BountifulLib plugin.
 * Handles plugin initialization, event registration, and custom block management.
 */
public final class Krimson extends JavaPlugin implements Listener {
    @Getter
    private static Krimson instance;
    @Getter
    private static TaskScheduler scheduler;
    @Getter
    private static FileConfiguration configuration;

    private static boolean isReloaded = false;

    public static Set<ItemDisplay> trackedDisplays = Collections.newSetFromMap(new WeakHashMap<>());

    public static NamespacedKey customBlockKey;

    public static CustomBlocksList<CustomBlock> customBlocks = new CustomBlocksList<>();

    public static Set<Chunk> processedChunks = new HashSet<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        saveDefaultConfig();
        configuration = getConfig();

        scheduler = UniversalScheduler.getScheduler(this);

        customBlockKey = new NamespacedKey(getInstance(), Keys.CUSTOM_BLOCK_KEY);

        isReloaded = !Bukkit.getOnlinePlayers().isEmpty();
        if(isReloaded) {
            customBlocks.clear();
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    onChunkLoad(new ChunkLoadEvent(chunk, false));
                }
            }
        }

        // Events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new CustomBlockSuppressionListener(), this);
        getServer().getPluginManager().registerEvents(new CustomBlockActionListener(), this);
        getServer().getPluginManager().registerEvents(new LightSourcePreventionListener(), this);

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

        // Check the ItemDisplay entities diff between old tick and actual tick and trigger a method while specifying the removed ItemDisplay
        // Sort of backport of EntityRemoveEvent for before 1.20.4
        getScheduler().runTaskTimer(() -> {
            Iterator<ItemDisplay> iterator = trackedDisplays.iterator();
            while (iterator.hasNext()) {
                ItemDisplay nextDisplayEntity = iterator.next();
                if (nextDisplayEntity.isValid()) {
                    continue;
                }

                iterator.remove();
                CustomBlockSuppressionListener.onCustomBlockDeath(nextDisplayEntity, true);
            }
        }, 1L, 1L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Goodbye from Krimson API!");
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
            getLogger().info("Adding existing custom blocks for " + chunk.getWorld().getName() + " chunk " + chunk.getX() + ", " + chunk.getZ() + "!");
            for (ItemDisplay itemDisplay : itemDisplays) {
                if (itemDisplay.getBrightness() == null) continue;

                CustomBlock e1 = new CustomBlockTypeChecker(itemDisplay).get();
                System.out.println(e1.getClass());
                gotCustomBlocks.add(e1);
            }
            customBlocks.addAll(gotCustomBlocks);

            getLogger().info("Added " + gotCustomBlocks.size() + " existing custom blocks for " + chunk.getWorld().getName() + " chunk " + chunk.getX() + ", " + chunk.getZ() + "!");
        }

        processedChunks.add(chunk);
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
        return itemDisplay.isValid() && itemDisplay.getPersistentDataContainer().has(customBlockKey, PersistentDataType.BYTE);
    }
}