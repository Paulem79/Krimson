package ovh.paulem.krimson.listeners;

import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

public class CustomBlockSuppressionListener implements Listener {
    public static void onCustomBlockDeath(ItemDisplay itemDisplay, boolean doCheck) {
        if(doCheck || Krimson.isCustomBlock(itemDisplay)) {
            CustomBlockUtils.removeDisplay(itemDisplay);
        }
    }

    @EventHandler
    public void onCustomBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        CustomBlockUtils.handleBlockSuppression(block, e);
    }

    @EventHandler
    public void onCustomBlockFromTo(BlockFormEvent e) {
        Block block = e.getBlock();
        CustomBlockUtils.handleBlockSuppression(block, e);
    }

    @EventHandler
    public void onCustomBlockExplosion(BlockExplodeEvent e) {
        for (Block block : e.blockList()) {
            CustomBlockUtils.handleBlockSuppression(block, e);
        }
    }

    @EventHandler
    public void onCustomBlockPistonExtend(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            CustomBlockUtils.handleBlockSuppression(block, e);
        }
    }

    @EventHandler
    public void onCustomBlockPistonExtend(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            CustomBlockUtils.handleBlockSuppression(block, e);
        }
    }


}
