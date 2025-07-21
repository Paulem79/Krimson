package ovh.paulem.krimson.listeners;

import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import ovh.paulem.krimson.blocks.CustomBlock;
import ovh.paulem.krimson.blocks.InventoryCustomBlock;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.InventoryBlockStartEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;

public class CustomBlockActionListener implements Listener {
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if(clickedBlock == null || clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            customBlock.onInteract(event);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block clickedBlock = event.getBlockPlaced();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            customBlock.onPlace(event);
        }
    }

    // ---------------------- INVENTORY PART ----------------------
    @EventHandler
    public void onGuiOpen(InventoryOpenEvent event) {
        Location inventoryLocation = event.getInventory().getLocation();
        if(inventoryLocation == null) {
            return;
        }

        Block clickedBlock = inventoryLocation.getBlock();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            if(customBlock instanceof InventoryCustomBlock inventoryCustomBlock){
                inventoryCustomBlock.onGuiOpen(event);
            }
        }
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        Location inventoryLocation = event.getInventory().getLocation();
        if(inventoryLocation == null) {
            return;
        }

        Block clickedBlock = inventoryLocation.getBlock();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            if(customBlock instanceof InventoryCustomBlock inventoryCustomBlock) {
                inventoryCustomBlock.onGuiClose(event);
            }
        }
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        Location inventoryLocation = event.getInventory().getLocation();
        if(inventoryLocation == null) {
            return;
        }

        Block clickedBlock = inventoryLocation.getBlock();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            if(customBlock instanceof InventoryCustomBlock inventoryCustomBlock) {
                inventoryCustomBlock.onGuiClick(event);
            }
        }
    }

    @EventHandler
    public void onGuiDrag(InventoryDragEvent event) {
        Location inventoryLocation = event.getInventory().getLocation();
        if(inventoryLocation == null) {
            return;
        }

        Block clickedBlock = inventoryLocation.getBlock();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            if(customBlock instanceof InventoryCustomBlock inventoryCustomBlock) {
                inventoryCustomBlock.onGuiDrag(event);
            }
        }
    }

    @EventHandler
    public void onGuiMoveItemFrom(InventoryMoveItemEvent event) {
        Location inventoryLocation = event.getSource().getLocation();
        if(inventoryLocation == null) {
            return;
        }

        Block clickedBlock = inventoryLocation.getBlock();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            if(customBlock instanceof InventoryCustomBlock inventoryCustomBlock) {
                inventoryCustomBlock.onGuiMoveItem(event);
            }
        }
    }

    @EventHandler
    public void onGuiMoveItemTo(InventoryMoveItemEvent event) {
        Location inventoryLocation = event.getDestination().getLocation();
        if(inventoryLocation == null) {
            return;
        }

        Block clickedBlock = inventoryLocation.getBlock();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            if(customBlock instanceof InventoryCustomBlock inventoryCustomBlock) {
                inventoryCustomBlock.onGuiMoveItem(event);
            }
        }
    }

    @EventHandler
    public void onGuiPickupItem(InventoryPickupItemEvent event) {
        Location inventoryLocation = event.getInventory().getLocation();
        if(inventoryLocation == null) {
            return;
        }

        Block clickedBlock = inventoryLocation.getBlock();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            if(customBlock instanceof InventoryCustomBlock inventoryCustomBlock) {
                inventoryCustomBlock.onGuiPickupItem(event);
            }
        }
    }

    @EventHandler
    public void onGuiBlockStart(InventoryBlockStartEvent event) {
        Block clickedBlock = event.getBlock();
        if(clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        Entity entity = CustomBlockUtils.getDisplayFromBlock(clickedBlock);
        if(entity == null) {
            return;
        }

        if(Krimson.isCustomBlock(entity)) {
            CustomBlock customBlock = CustomBlockUtils.getCustomBlockFromEntity(entity);

            if(customBlock == null) {
                return;
            }

            if(customBlock instanceof InventoryCustomBlock inventoryCustomBlock) {
                inventoryCustomBlock.onGuiBlockStart(event);
            }
        }
    }
}
