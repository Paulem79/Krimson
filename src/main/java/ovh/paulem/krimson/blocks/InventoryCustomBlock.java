package ovh.paulem.krimson.blocks;

import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.block.CampfireStartEvent;
import org.bukkit.event.block.InventoryBlockStartEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

public abstract class InventoryCustomBlock extends CustomBlock {
    public InventoryCustomBlock(Material blockInside, ItemStack displayedItem, int emittingLightLevel) {
        super(blockInside, displayedItem, emittingLightLevel);
        Preconditions.checkArgument(blockInside.isInteractable(), "The block inside must be interactable!");
    }

    public InventoryCustomBlock(Material blockInside, ItemStack displayedItem, int emittingLightLevel, ItemDisplay itemDisplay) {
        super(blockInside, displayedItem, emittingLightLevel, itemDisplay);
        Preconditions.checkArgument(blockInside.isInteractable(), "The block inside must be interactable!");
    }

    /**
     * Called when the custom block inventory is opened by a player.
     */
    public void onGuiOpen(InventoryOpenEvent event) {
    }
    /**
     * Called when the custom block inventory is closed by a player.
     */
    public void onGuiClose(InventoryCloseEvent event) {
    }
    /**
     * Called when a custom block inventory's slot is clicked by a player.
     */
    public void onGuiClick(InventoryClickEvent event) {
    }
    /**
     * Called when the player drags an item in their cursor across the custom block inventory.
     */
    public void onGuiDrag(InventoryDragEvent event) {
    }

    /**
     * Called when some entity or block (e.g. hopper) tries to move items directly from the custom block inventory to another or the invert.
     */
    public void onGuiMoveItem(InventoryMoveItemEvent event) {
    }

    /**
     * Called when the custom block hopper or hopper minecart picks up a dropped item.
     */
    public void onGuiPickupItem(InventoryPickupItemEvent event) {
    }
    /**
     * Used when:
     * <ul>
     * <li>A custom block Furnace starts smelting {@link FurnaceStartSmeltEvent}</li>
     * <li>A custom block Brewing-Stand starts brewing {@link BrewingStartEvent}</li>
     * <li>A custom block Campfire starts cooking {@link CampfireStartEvent}</li>
     * </ul>
     */
    public void onGuiBlockStart(InventoryBlockStartEvent event) {
    }
}
