package net.paulem.krimson;

import com.jeff_media.customblockdata.CustomBlockData;
import net.paulem.krimson.listeners.*;
import net.paulem.krimson.resourcepack.creator.ResourcePackKt;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.PluginManager;
import net.paulem.krimson.blocks.Blocks;
import net.paulem.krimson.commands.CommandKrimson;
import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.items.Items;
import net.paulem.krimson.properties.PDCWrapper;
import net.paulem.krimson.regions.CustomBlockTracker;
import net.paulem.krimson.resourcepack.ResourcePackHosting;

// TODO : Seems like PDC is slow https://github.com/PaperMC/Paper/pull/3359

/**
 * Main class for the BountifulLib plugin.
 * Handles plugin initialization, event registration, and custom block management.
 */
public final class Krimson extends KrimsonPlugin<Krimson> implements Listener {
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
        return properties.has(Keys.CUSTOM_BLOCK_KEY) &&
                properties.has(Keys.IDENTIFIER_KEY) &&
                properties.has(Keys.BLOCK_INSIDE_KEY) &&
                properties.has(Keys.DISPLAYED_ITEM_KEY) && !block.getType().isAir();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        getLogger().info("Hello from Krimson API!");

        customBlocks = new CustomBlockTracker();
        getLogger().info("Scheduled ticking!");

        Items.init();
        Blocks.init();

        // Events
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(this, this);
        pluginManager.registerEvents(new CustomBlockSuppressionListener(), this);
        pluginManager.registerEvents(new CustomBlockActionListener(), this);
        pluginManager.registerEvents(new LightSourcePreventionListener(), this);
        pluginManager.registerEvents(new BlockItemHandlerListener(), this);
        pluginManager.registerEvents(new MigrationListener(), this);
        CustomBlockData.registerListener(this);

        // Commands
        PluginCommand krimsonCommand = getCommand("krimson");
        CommandKrimson krimsonCommandInstance = new CommandKrimson();
        krimsonCommand.setExecutor(krimsonCommandInstance);
        krimsonCommand.setTabCompleter(krimsonCommandInstance);
        getLogger().info("Registered commands!");

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