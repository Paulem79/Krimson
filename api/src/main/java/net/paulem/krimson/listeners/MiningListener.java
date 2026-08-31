package net.paulem.krimson.listeners;

import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.blocks.mining.MiningManager;
import net.paulem.krimson.blocks.mining.MiningProperties;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Hands the mining of custom blocks over to {@link MiningManager} whenever the block declares its own
 * {@link MiningProperties}. Blocks left on {@link MiningProperties#INHERIT} keep their vanilla behaviour.
 */
public class MiningListener implements Listener {
    private static boolean enabled() {
        return KrimsonPlugin.getConfiguration().getBoolean("customMining", true);
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (!enabled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!KrimsonAPI.isCustomBlockFromWatcher(block)) return;

        CustomBlock customBlock = KrimsonAPI.customBlocks.getBlockAt(block);
        if (customBlock == null) return;

        MiningProperties properties = customBlock.resolveMiningProperties();
        // No custom hardness declared: leave the block to the vanilla mining of its carrier material.
        if (properties.inherits()) return;

        MiningManager manager = MiningManager.getInstance();

        if (player.getGameMode() == GameMode.CREATIVE || properties.hardness() == 0) {
            manager.end(player);
            event.setInstaBreak(true);
            return;
        }

        event.setCancelled(true);
        manager.start(player, block, customBlock, properties);
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        MiningManager.getInstance().end(event.getPlayer());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        MiningManager.getInstance().end(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        MiningManager.getInstance().end(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        MiningManager.getInstance().end(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Removes the injected mining fatigue so it is not carried over to the next login.
        MiningManager.getInstance().end(event.getPlayer());
    }
}
