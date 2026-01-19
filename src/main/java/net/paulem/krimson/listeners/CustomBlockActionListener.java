package net.paulem.krimson.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.blocks.custom.InventoryCustomBlock;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// FIXME: The last custom block in the inventory (when stack is at count 1) placed on an already placed custom block's face will not place the new custom block, remove it from the inventory, and trigger the opening of the existing custom block's inventory GUI.
public class CustomBlockActionListener implements Listener {
    public static Set<UUID> notAllowed = new HashSet<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.useInteractedBlock() == Event.Result.DENY || notAllowed.contains(player.getUniqueId())) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return; // Only handle right or left click actions on blocks
        }

        EquipmentSlot slot = event.getHand();
        if (slot != EquipmentSlot.HAND && slot != EquipmentSlot.OFF_HAND) {
            return; // Only handle main hand interactions
        }

        if (player.isSneaking()) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && !item.getType().isAir()) {
                return;
            }
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        if (Krimson.isCustomBlockFromWatcher(clickedBlock)) {
            CustomBlock customBlock = Krimson.customBlocks.getBlockAt(clickedBlock);

            if (customBlock == null) {
                return;
            }

            customBlock.onInteract(event);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block clickedBlock = event.getBlockPlaced();
        if (clickedBlock.isEmpty() || clickedBlock.isLiquid()) {
            return;
        }

        if (Krimson.isCustomBlockFromWatcher(clickedBlock)) {
            CustomBlock customBlock = Krimson.customBlocks.getBlockAt(clickedBlock);

            if (customBlock == null) {
                return;
            }

            customBlock.onPlace(event);
        }
    }

    // ---------------------- INVENTORY PART ----------------------
    @EventHandler
    public void onGuiOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }

        if (holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiOpen(event);
        }
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }

        if (holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiClose(event);
        }
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }

        if (holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiClick(event);
        }
    }

    @EventHandler
    public void onGuiDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }

        if (holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiDrag(event);
        }
    }

    @EventHandler
    public void onGuiMoveItemFrom(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getSource().getHolder();
        if (holder == null) {
            return;
        }

        if (holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiMoveItem(event);
        }
    }

    @EventHandler
    public void onGuiMoveItemTo(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getDestination().getHolder();
        if (holder == null) {
            return;
        }

        if (holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiMoveItem(event);
        }
    }

    @EventHandler
    public void onGuiPickupItem(InventoryPickupItemEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }

        if (holder instanceof InventoryCustomBlock.InventoryCustomBlockHolder customBlockHolder) {
            InventoryCustomBlock customBlock = customBlockHolder.getCustomBlock();

            customBlock.onGuiPickupItem(event);
        }
    }
}
