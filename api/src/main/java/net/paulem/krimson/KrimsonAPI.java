package net.paulem.krimson;

import com.jeff_media.customblockdata.CustomBlockData;
import net.paulem.krimson.blocks.noteblock.NoteBlockStates;
import net.paulem.krimson.blocks.noteblock.listeners.NoteBlockListener;
import net.paulem.krimson.commands.StandCommand;
import net.paulem.krimson.listeners.*;
import net.paulem.krimson.mobs.CustomMobs;
import net.paulem.krimson.mobs.listeners.CustomMobListener;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.PluginManager;
import net.paulem.krimson.commands.CommandKrimson;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.PDCWrapper;
import net.paulem.krimson.regions.CustomBlockTracker;
import net.paulem.krimson.resourcepack.ResourcePackHosting;
import org.bukkit.command.CommandExecutor;

import java.util.logging.Logger;

// NOTE: PDC (PersistentDataContainer) has known performance issues on PaperMC.
// See: https://github.com/PaperMC/Paper/pull/3359
// Consider caching frequently accessed data or using alternative storage for performance-critical operations.

/**
 * Main class for the Krimson plugin.
 * Handles plugin initialization, event registration, and custom block management.
 */
public class KrimsonAPI<T extends KrimsonPlugin<T>> implements Listener {
    public static ResourcePackHosting packHosting;
    public static CustomBlockTracker customBlocks;

    /**
     * Checks if the given block is a custom block.
     */
    public static boolean isCustomBlockFromWatcher(Block block) {
        return customBlocks.getBlockAt(block) != null;
    }

    public static boolean isCustomBlock(Block block) {
        PDCWrapper properties = new PDCWrapper(block);
        return properties.has(Keys.CUSTOM_BLOCK) &&
                properties.has(Keys.IDENTIFIER) &&
                properties.has(Keys.BLOCK_INSIDE) &&
                properties.has(Keys.DISPLAYED_ITEM) &&
                !block.getType().isAir();
    }

    private final KrimsonPlugin<T> plugin;

    public KrimsonAPI(KrimsonPlugin<T> plugin) {
        this.plugin = plugin;
    }

    public void init(boolean registerCommand) {
        getLogger().info("Hello from Krimson API!");

        customBlocks = new CustomBlockTracker();
        getLogger().info("Scheduled ticking!");

        // Must run before initBlocks(): registering a note block backed block allocates a blockstate.
        NoteBlockStates.load(plugin.getDataFolder());

        plugin.initItems();
        plugin.initBlocks();
        NoteBlockStates.save();
        plugin.initModels();
        plugin.initMobs();
        CustomMobs.REGISTRY.freeze();
        CustomMobs.init();
        plugin.initSounds();
        plugin.initUIs();

        // Events
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(this, plugin);
        pluginManager.registerEvents(new CustomBlockSuppressionListener(), plugin);
        pluginManager.registerEvents(new CustomBlockActionListener(), plugin);
        pluginManager.registerEvents(new LightSourcePreventionListener(), plugin);
        pluginManager.registerEvents(new BlockItemHandlerListener(), plugin);
        pluginManager.registerEvents(new MigrationListener(), plugin);
        pluginManager.registerEvents(new CustomMobListener(), plugin);
        // Registered last on purpose: it denies note block interactions, and the listeners above must get
        // their turn at the same priority first.
        pluginManager.registerEvents(new NoteBlockListener(), plugin);
        CustomBlockData.registerListener(plugin);

        // Commands
        if(registerCommand) {
            PluginCommand krimsonCommand = plugin.getCommand("krimson");
            CommandKrimson krimsonCommandInstance = new CommandKrimson();
            krimsonCommand.setExecutor(krimsonCommandInstance);
            krimsonCommand.setTabCompleter(krimsonCommandInstance);

            PluginCommand standCommand = plugin.getCommand("stand");
            StandCommand standCommandInstance = new StandCommand(plugin);
            standCommand.setExecutor(standCommandInstance);
            standCommand.setTabCompleter(standCommandInstance);

            // Register UI command if it exists
            try {
                PluginCommand uiCommand = plugin.getCommand("ui");
                if (uiCommand != null) {
                    Class<?> uiCommandClass = Class.forName("net.paulem.krimsontest.commands.UICommand");
                    Object uiCommandInstance = uiCommandClass.getDeclaredConstructor().newInstance();
                    uiCommand.setExecutor((CommandExecutor) uiCommandInstance);
                }
            } catch (ClassNotFoundException e) {
                // UI command class not found - this is expected if the test plugin isn't using it
                getLogger().fine("UI command class not found - skipping UI command registration");
            } catch (Exception e) {
                getLogger().warning("Failed to register UI command: " + e.getMessage());
            }

            getLogger().info("Registered commands!");
        }

        packHosting = new ResourcePackHosting();
        pluginManager.registerEvents(packHosting, plugin);
        packHosting.start();

        getLogger().info("Finished loading Krimson API!");
    }

    public void stop() {
        packHosting.stop();
        CustomMobs.shutdown();

        getLogger().info("Goodbye from Krimson API!");
    }

    public Logger getLogger() {
        return plugin.getLogger();
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

        customBlocks.handleChunkLoad(chunk);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        customBlocks.handleChunkUnload(chunk);
    }
}