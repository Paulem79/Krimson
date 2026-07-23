package net.paulem.krimson.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.blocks.custom.LightBlock;

// Note: Falling sand/gravel can temporarily obfuscate light sources. This is a known limitation.
// Potential solutions would require either:
// 1. Making falling blocks pass through custom light blocks (unrealistic behavior)
// 2. Implementing a delayed check to remove obfuscating blocks
// 3. Using a different light implementation that isn't affected by block placement
// Current approach prioritizes realistic physics over perfect light visibility.
public class LightSourcePreventionListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block blockPlaced = event.getBlockPlaced();
        CustomBlock customBlock = KrimsonAPI.customBlocks.getBlockAt(blockPlaced.getRelative(BlockFace.DOWN));

        if (customBlock instanceof LightBlock) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokeBlock = event.getBlock();
        CustomBlock customBlock = KrimsonAPI.customBlocks.getBlockAt(brokeBlock.getRelative(BlockFace.DOWN));

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
