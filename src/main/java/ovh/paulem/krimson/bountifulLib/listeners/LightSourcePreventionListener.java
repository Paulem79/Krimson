package ovh.paulem.krimson.bountifulLib.listeners;

import ovh.paulem.krimson.bountifulLib.BountifulLib;
import ovh.paulem.krimson.bountifulLib.CustomBlockUtils;
import ovh.paulem.krimson.bountifulLib.blocks.CustomBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class LightSourcePreventionListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        for (CustomBlock customBlock : BountifulLib.customBlocks) {
            Location customBlockLocation = CustomBlockUtils.getBlockFromDisplay(customBlock.getSpawnedDisplay()).getLocation();
            if(customBlockLocation.add(0, 1, 0).equals(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        for (CustomBlock customBlock : BountifulLib.customBlocks) {
            Location customBlockLocation = CustomBlockUtils.getBlockFromDisplay(customBlock.getSpawnedDisplay()).getLocation();
            Block lightBlock = customBlockLocation.add(0, 1, 0).getBlock();

            if(lightBlock.getLocation().equals(block.getLocation())) {
                if(lightBlock.getType() != Material.LIGHT) {
                    lightBlock.setType(Material.LIGHT);
                    Light light = (Light) lightBlock.getBlockData();
                    light.setLevel(customBlock.getEmittingLightLevel());
                    lightBlock.setBlockData(light);
                    lightBlock.getState().update();
                }
                event.setCancelled(true);
                return;
            }
        }
    }
}
