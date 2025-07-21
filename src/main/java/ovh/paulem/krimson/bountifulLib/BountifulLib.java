package ovh.paulem.krimson.bountifulLib;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import lombok.Getter;
import ovh.paulem.krimson.bountifulLib.blocks.CustomBlock;
import ovh.paulem.krimson.bountifulLib.blocks.LightBlock;
import ovh.paulem.krimson.bountifulLib.commands.CommandDisplay;
import ovh.paulem.krimson.bountifulLib.listeners.CustomBlockActionListener;
import ovh.paulem.krimson.bountifulLib.listeners.CustomBlockSuppressionListener;
import ovh.paulem.krimson.bountifulLib.listeners.LightSourcePreventionListener;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

// TODO : Add place custom block from inventory
/**
 * Main class for the BountifulLib plugin.
 * Handles plugin initialization, event registration, and custom block management.
 */
public final class BountifulLib extends JavaPlugin implements Listener {
    @Getter
    private static BountifulLib instance;
    @Getter
    private static TaskScheduler scheduler;
    @Getter
    private static FileConfiguration configuration;

    private static boolean isReloaded = false;

    public static Set<ItemDisplay> trackedDisplays = Collections.newSetFromMap(new WeakHashMap<>());

    public static NamespacedKey customBlockKey;

    public static LinkedList<CustomBlock> customBlocks = new LinkedList<>();

    /**
     * Called when the plugin is enabled.
     * Initializes the plugin, registers events, and schedules tasks.
     */
    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        saveDefaultConfig();
        configuration = getConfig();

        scheduler = UniversalScheduler.getScheduler(this);

        customBlockKey = new NamespacedKey(getInstance(), "customblock");

        isReloaded = !Bukkit.getOnlinePlayers().isEmpty();
        if(isReloaded) {
            customBlocks.clear();
            for (World world : Bukkit.getWorlds()) {
                onWorldLoad(new WorldLoadEvent(world));
            }
        }

        // Events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new CustomBlockSuppressionListener(), this);
        getServer().getPluginManager().registerEvents(new CustomBlockActionListener(), this);
        getServer().getPluginManager().registerEvents(new LightSourcePreventionListener(), this);

        // Main
        getLogger().info("Hello from BountifulLib!");

        getScheduler().runTaskTimerAsynchronously(() -> {
            for (CustomBlock customBlock : customBlocks) {
                CustomBlock.tickPredicate.apply(customBlock);
            }
        }, 1L, 1L);
        getLogger().info("Scheduled brightness !");

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

    /**
     * Called when the plugin is disabled.
     * Handles plugin shutdown logic.
     */
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Goodbye from BountifulLib!");
    }

    /**
     * Event handler for world load events.
     * Adds existing custom blocks in the loaded world to the customBlocks list.
     *
     * @param e the WorldLoadEvent
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        World world = e.getWorld();

        LinkedList<CustomBlock> gotCustomBlocks = new LinkedList<>();

        Collection<ItemDisplay> itemDisplays = world.getEntitiesByClass(ItemDisplay.class).stream().filter(BountifulLib::isCustomBlock).toList();
        if(!itemDisplays.isEmpty())
        {
            getLogger().info("Adding existing custom blocks for " + world.getName() + "...");
            for (ItemDisplay itemDisplay : itemDisplays) {
                if (itemDisplay.getBrightness() == null) continue;

                gotCustomBlocks.add(new LightBlock(itemDisplay));
            }
            customBlocks.addAll(gotCustomBlocks);

            getLogger().info("Added " + gotCustomBlocks.size() + " existing custom blocks for " + world.getName() + " !");
        }
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