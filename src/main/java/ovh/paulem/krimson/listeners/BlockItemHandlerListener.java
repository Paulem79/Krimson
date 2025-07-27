package ovh.paulem.krimson.listeners;

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
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.blocks.InventoryCustomBlock;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.items.BlockItem;
import ovh.paulem.krimson.items.Items;
import ovh.paulem.krimson.utils.BlockUtils;
import ovh.paulem.krimson.utils.CustomBlockUtils;

import java.util.Arrays;
import java.util.Optional;

public class BlockItemHandlerListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(PlayerInteractEvent event) {
        if(event.useInteractedBlock() == Event.Result.DENY || event.useItemInHand() == Event.Result.DENY) return;

        Action action = event.getAction();
        if(action != Action.RIGHT_CLICK_BLOCK) {
            return; // Only handle right or left click actions on blocks
        }

        Player player = event.getPlayer();
        
        if(player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.ADVENTURE) {
            return; // Do not allow block placement in spectator or adventure mode
        }
        
        Block block = event.getClickedBlock();
        if(block == null) return;

        ItemStack item = event.getItem();
        if(item == null || item.getItemMeta() == null || !item.getItemMeta().hasItemModel()) return;

        BlockFace face = event.getBlockFace();

        if(!BlockUtils.canPlaceOn(player, block.getRelative(face))) {
            return;
        }

        EquipmentSlot slot = event.getHand();
        if (slot != EquipmentSlot.HAND) {
            return; // Only handle main hand interactions
        }

        // If there is a custom inventory block or a block that opens an inventory at the clicked location (and the player isn't sneaking), do not allow placement
        if(
                (CustomBlockUtils.getCustomBlockFromLoc(block.getLocation()) instanceof InventoryCustomBlock || block.getState() instanceof TileState) &&
                        !player.isSneaking()
        ) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        @Nullable String identifier = pdc.get(new NamespacedKey(Krimson.getInstance(), Keys.IDENTIFIER_KEY), PersistentDataType.STRING);

        if(identifier == null) {
            return;
        }

        NamespacedKey key = NamespacedKey.fromString(identifier);

        if (key == null) {
            Krimson.getInstance().getLogger().warning("Invalid item identifier: " + identifier + " for item: " + item.getType() + " used by player: " + player.getName());
            return;
        }

        BlockItem blockItem = Items.REGISTRY.getOrThrow(key);

        if(player.getGameMode() != GameMode.CREATIVE) {
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

        Block toPlace = block.getRelative(face);

        // If the block is not solid, use the clicked block instead (like for grass)
        if(!block.getType().isSolid()) {
            toPlace = block;
        }

        blockItem.getAction().accept(blockItem, player, toPlace.getLocation());

        CustomBlockActionListener.notAllowed.add(player.getUniqueId());

        Krimson.getScheduler().runTaskAsynchronously(() -> {
            CustomBlockActionListener.notAllowed.remove(player.getUniqueId());
        });

        final Block finalToPlace = toPlace;
        Krimson.getScheduler().runTaskLater(() -> {
            Krimson.retryCustomBlockFromBlock(finalToPlace, player);
        }, 2L);

        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if(CustomBlockActionListener.notAllowed.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTryCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();

        if(Arrays.stream(matrix).anyMatch(item -> {
            if(item == null || item.getItemMeta() == null || !item.getItemMeta().hasItemModel()) return false;

            ItemMeta meta = item.getItemMeta();

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            @Nullable String identifier = pdc.get(new NamespacedKey(Krimson.getInstance(), Keys.IDENTIFIER_KEY), PersistentDataType.STRING);

            if(identifier == null) {
                return false;
            }

            NamespacedKey key = NamespacedKey.fromString(identifier);

            if (key == null) {
                Krimson.getInstance().getLogger().warning("Invalid item identifier: " + identifier + " for item: " + item.getType() + " used by player: " + event.getView().getPlayer().getName());
                return false;
            }

            Optional<BlockItem> blockItem = Items.REGISTRY.get(key);

            return blockItem.isPresent();
        })) {
            event.getInventory().setResult(null);
        }
    }
}
