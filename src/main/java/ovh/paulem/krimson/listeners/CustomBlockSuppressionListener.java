package ovh.paulem.krimson.listeners;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityExplodeEvent;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

public class CustomBlockSuppressionListener implements Listener {
    public static void onCustomBlockDeath(ItemDisplay itemDisplay, boolean passCheck) {
        if(passCheck || Krimson.isCustomBlock(itemDisplay)) {
            CustomBlockUtils.removeDisplay(itemDisplay);
        }
    }

    @EventHandler
    public void onCustomBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if(Krimson.isCustomBlock(block)) {
            CustomBlockUtils.handleBlockSuppression(block, e);
        }
    }

    @EventHandler
    public void onCustomBlockFromTo(BlockFormEvent e) {
        Block block = e.getBlock();
        if(Krimson.isCustomBlock(block)) {
            CustomBlockUtils.handleBlockSuppression(block, e);
        }
    }

    @EventHandler
    public void onCustomBlockExplosion(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            if(Krimson.isCustomBlock(block)) {
                block.setType(Material.AIR);
                CustomBlockUtils.handleBlockSuppression(block, e);
            }
        }
    }

    // TODO : Make custom blocks work with piston
    @EventHandler
    public void onCustomBlockPistonExtend(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            if(Krimson.isCustomBlock(block)) {
                CustomBlockUtils.handleBlockSuppression(block, e);
            }
        }
    }

    @EventHandler
    public void onCustomBlockPistonExtend(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            if(Krimson.isCustomBlock(block)) {
                CustomBlockUtils.handleBlockSuppression(block, e);
            }
        }
    }


}
