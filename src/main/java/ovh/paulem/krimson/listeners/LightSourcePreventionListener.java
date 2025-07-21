package ovh.paulem.krimson.listeners;

import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.blocks.LightBlock;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

// TODO: Falling sand can still obfuscate the light source, need to find a way to prevent that, but I don't want to make sand fly magically
public class LightSourcePreventionListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        for (LightBlock customBlock : Krimson.customBlocks.getAll(LightBlock.class)) {
            Location customBlockLocation = CustomBlockUtils.getBlockFromDisplay(customBlock.getSpawnedDisplay()).getLocation();
            if(customBlockLocation.add(0, 1, 0).equals(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokeBlock = event.getBlock();
        for (LightBlock customBlock : Krimson.customBlocks.getAll(LightBlock.class)) {
            Location customBlockLocation = CustomBlockUtils.getBlockFromDisplay(customBlock.getSpawnedDisplay()).getLocation();
            Block lightBlock = customBlockLocation.add(0, 1, 0).getBlock();

            if(lightBlock.getLocation().equals(brokeBlock.getLocation())) {
                if(lightBlock.getType() != Material.LIGHT) {
                    customBlock.spawnLight();
                }
                event.setCancelled(true);
                return;
            }
        }
    }
}
