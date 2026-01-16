package net.paulem.krimson.listeners;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.items.CustomBlockItem;
import net.paulem.krimson.items.CustomItem;
import net.paulem.krimson.items.Items;
import net.paulem.krimson.utils.BlockUtils;
import net.paulem.krimson.utils.CustomBlockUtils;

import java.util.Arrays;
import java.util.Optional;

public class BlockItemHandlerListener implements Listener {
    private static void cancelCBActionForNextTick(Player player) {
        CustomBlockActionListener.notAllowed.add(player.getUniqueId());

        Krimson.getScheduler().runTaskAsynchronously(() -> {
            CustomBlockActionListener.notAllowed.remove(player.getUniqueId());
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Event.Result.DENY || event.useItemInHand() == Event.Result.DENY) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return; // Only handle right or left click actions on blocks
        }

        EquipmentSlot slot = event.getHand();
        if (slot != EquipmentSlot.HAND) {
            return; // Only handle main hand interactions
        }

        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.ADVENTURE) {
            return; // Do not allow block placement in spectator or adventure mode
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // If there is a custom inventory block or a block that opens an inventory at the clicked location (and the player isn't sneaking), do not allow placement
        if (
                (CustomBlockUtils.getCustomBlockFromLoc(clickedBlock.getLocation()) instanceof InventoryCustomBlock || clickedBlock.getState() instanceof TileState) &&
                        !player.isSneaking()
        ) {
            ItemStack item = player.getInventory().getItem(slot);

            // If the player is not sneaking and doesn't have an item, open inventory
            if (item == null || item.getType().isAir()) {
                return;
            }
        }

        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null || !item.getItemMeta().hasItemModel()) return;

        BlockFace face = event.getBlockFace();

        Block toPlace = clickedBlock.getRelative(face);

        // If the block is not solid and passable, use the clicked block instead (like for grass)
        if (!clickedBlock.getType().isSolid() && clickedBlock.isPassable()) {
            toPlace = clickedBlock;
        }

        if (!BlockUtils.canPlaceOn(player, toPlace)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        @Nullable String identifier = pdc.get(new NamespacedKey(Krimson.getInstance(), Keys.IDENTIFIER_KEY), PersistentDataType.STRING);

        if (identifier == null) {
            return;
        }

        NamespacedKey key = NamespacedKey.fromString(identifier);

        if (key == null) {
            Krimson.getInstance().getLogger().warning("Invalid item identifier: " + identifier + " for item: " + item.getType() + " used by player: " + player.getName());
            return;
        }

        CustomItem blockItem = Items.REGISTRY.getOrThrow(key);

        if(!(blockItem instanceof CustomBlockItem customBlockItem)) {
            Krimson.getInstance().getLogger().warning("Item " + blockItem.getKey() + " is not a CustomBlockItem, cannot place it as a block.");
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setUseItemInHand(Event.Result.DENY);

            // If there is only one item remaining, remove it from the player's inventory
            if (item.getAmount() == 1) {
                player.getInventory().setItem(slot, null);
                return;
            }

            // Remove one item from the player's inventory
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        }

        customBlockItem.getAction().accept(customBlockItem.getCustomBlock(), player, toPlace.getLocation());

        cancelCBActionForNextTick(player);

        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (CustomBlockActionListener.notAllowed.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTryCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();

        if (Arrays.stream(matrix).anyMatch(item -> {
            if (item == null || item.getItemMeta() == null || !item.getItemMeta().hasItemModel()) return false;

            ItemMeta meta = item.getItemMeta();

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            @Nullable String identifier = pdc.get(new NamespacedKey(Krimson.getInstance(), Keys.IDENTIFIER_KEY), PersistentDataType.STRING);

            if (identifier == null) {
                return false;
            }

            NamespacedKey key = NamespacedKey.fromString(identifier);

            if (key == null) {
                Krimson.getInstance().getLogger().warning("Invalid item identifier: " + identifier + " for item: " + item.getType() + " used by player: " + event.getView().getPlayer().getName());
                return false;
            }

            Optional<CustomItem> blockItem = Items.REGISTRY.get(key);

            return blockItem.isPresent();
        })) {
            event.getInventory().setResult(null);
        }
    }
}
