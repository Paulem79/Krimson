package net.paulem.krimson.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.blocks.custom.LightBlock;

// TODO: Falling sand can still obfuscate the light source, need to find a way to prevent that, but I don't want to make sand fly magically
public class LightSourcePreventionListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block blockPlaced = event.getBlockPlaced();
        CustomBlock customBlock = Krimson.customBlocks.getBlockAt(blockPlaced.getRelative(BlockFace.DOWN));

        if (customBlock instanceof LightBlock) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokeBlock = event.getBlock();
        CustomBlock customBlock = Krimson.customBlocks.getBlockAt(brokeBlock.getRelative(BlockFace.DOWN));

        if (customBlock instanceof LightBlock lightCustomBlock) {
            Block lightBlock = lightCustomBlock.getLightBlock();

            if (lightBlock.getType() != Material.LIGHT) {
                lightCustomBlock.spawnLight();
            } else {
                event.setCancelled(true);
            }
        }
    }
}
