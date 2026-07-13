package net.paulem.krimson.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.utils.CustomBlockUtils;

public class CustomBlockSuppressionListener implements Listener {

    @EventHandler
    public void onCustomBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (KrimsonAPI.isCustomBlockFromWatcher(block)) {
            CustomBlockUtils.handleBlockSuppression(block, e);
        }
    }

    @EventHandler
    public void onCustomBlockFromTo(BlockFormEvent e) {
        Block block = e.getBlock();
        if (KrimsonAPI.isCustomBlockFromWatcher(block)) {
            CustomBlockUtils.handleBlockSuppression(block, e);
        }
    }

    @EventHandler
    public void onCustomBlockExplosion(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (KrimsonAPI.isCustomBlockFromWatcher(block)) {
                block.setType(Material.AIR);
                CustomBlockUtils.handleBlockSuppression(block, e);
            }
        }
    }

    // TODO : Make custom blocks work with piston
    @EventHandler
    public void onCustomBlockPistonExtend(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            if (KrimsonAPI.isCustomBlock(block)) {
                CustomBlockUtils.handleBlockSuppression(block, e);
            }
        }
    }

    @EventHandler
    public void onCustomBlockPistonExtend(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            if (KrimsonAPI.isCustomBlock(block)) {
                CustomBlockUtils.handleBlockSuppression(block, e);
            }
        }
    }
}
