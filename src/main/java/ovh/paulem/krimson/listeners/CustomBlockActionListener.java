package ovh.paulem.krimson.listeners;

import org.bukkit.event.Event;
import org.bukkit.inventory.InventoryHolder;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import ovh.paulem.krimson.blocks.CustomBlock;
import ovh.paulem.krimson.blocks.InventoryCustomBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CustomBlockActionListener implements Listener {
    public static Set<UUID> notAllowed = new HashSet<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(event.useInteractedBlock() == Event.Result.DENY || notAllowed.contains(event.getPlayer().getUniqueId())) return;

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
        InventoryHolder holder = event.getInventory().getHolder();
        if(holder == null) {
            return;
        }

        if(holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiOpen(event);
        }
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if(holder == null) {
            return;
        }

        if(holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiClose(event);
        }
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if(holder == null) {
            return;
        }

        if(holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiClick(event);
        }
    }

    @EventHandler
    public void onGuiDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if(holder == null) {
            return;
        }

        if(holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiDrag(event);
        }
    }

    @EventHandler
    public void onGuiMoveItemFrom(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getSource().getHolder();
        if(holder == null) {
            return;
        }

        if(holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiMoveItem(event);
        }
    }

    @EventHandler
    public void onGuiMoveItemTo(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getDestination().getHolder();
        if(holder == null) {
            return;
        }

        if(holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiMoveItem(event);
        }
    }

    @EventHandler
    public void onGuiPickupItem(InventoryPickupItemEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if(holder == null) {
            return;
        }

        if(holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiPickupItem(event);
        }
    }
}
