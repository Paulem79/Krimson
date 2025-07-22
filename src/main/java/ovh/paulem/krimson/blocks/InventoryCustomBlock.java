package ovh.paulem.krimson.blocks;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.inventories.InventoryData;
import ovh.paulem.krimson.utils.CustomBlockUtils;
import ovh.paulem.krimson.utils.serialization.InventorySerialization;
import ovh.paulem.krimson.utils.properties.PropertiesField;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

// TODO : Compress only on inventory changes
// FIXME: there are still some difficulties with this approach, especially around parsing/saving it asynchronously which can become an issue if there are a lot of these and they have large contents, but for a basic approach this will be fine https://discord.com/channels/690411863766466590/741875863271899136/1397175434432614472 (saving in files, with pointers in PDC can be better)
public class InventoryCustomBlock extends CustomBlock {
    @Getter
    protected PropertiesField<Integer> inventorySize;
    private final int baseInventorySize;
    @Getter
    protected PropertiesField<String> inventoryTitle;
    private final String baseInventoryTitle;
    @Getter
    protected PropertiesField<byte[]> inventoryBase64;
    @Getter
    private Inventory inventory;

    public InventoryCustomBlock(Material blockInside, ItemStack displayedItem, int inventorySize, String inventoryTitle) {
        super(blockInside, displayedItem);

        this.baseInventorySize = inventorySize;
        this.baseInventoryTitle = inventoryTitle;
    }

    public InventoryCustomBlock(ItemDisplay itemDisplay) {
        super(itemDisplay);

        this.inventorySize = new PropertiesField<>(Keys.INVENTORY_SIZE, properties, PersistentDataType.INTEGER);
        this.baseInventorySize = this.inventorySize.get();

        this.inventoryTitle = new PropertiesField<>(Keys.INVENTORY_TITLE, properties, PersistentDataType.STRING);
        this.baseInventoryTitle = this.inventoryTitle.get();

        this.inventoryBase64 = new PropertiesField<>(Keys.INVENTORY_BASE64, properties, PersistentDataType.BYTE_ARRAY);
        try {
            this.inventory = InventorySerialization.deserialize(this.inventoryBase64.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void spawn(Location blockLoc) {
        super.spawn(blockLoc);

        this.inventorySize = new PropertiesField<>(Keys.INVENTORY_SIZE, baseInventorySize);
        properties.set(this.inventorySize);

        this.inventoryTitle = new PropertiesField<>(Keys.INVENTORY_TITLE, baseInventoryTitle);
        properties.set(this.inventoryTitle);

        inventory = Krimson.getInstance().getServer().createInventory(
                new InventoryCustomBlockHolder(this),
                this.inventorySize.get(),
                this.inventoryTitle.get()
        );

        inventoryBase64 = new PropertiesField<>(Keys.INVENTORY_BASE64, InventorySerialization.serialize(new InventoryData(inventory, this.inventoryTitle.get())));
        properties.set(inventoryBase64);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if(action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        player.openInventory(inventory);

        event.setCancelled(true);
    }

    public void onGuiOpen(InventoryOpenEvent event) {
    }

    public void onGuiClose(InventoryCloseEvent event) {
        this.inventoryBase64 = new PropertiesField<>(Keys.INVENTORY_BASE64, InventorySerialization.serialize(new InventoryData(event.getInventory(), this.inventoryTitle.get())));
        this.properties.set(this.inventoryBase64);

        this.inventory = event.getInventory();
    }

    public void onGuiClick(InventoryClickEvent event) {
    }

    public void onGuiDrag(InventoryDragEvent event) {
    }

    public void onGuiMoveItem(InventoryMoveItemEvent event) {
    }

    public void onGuiPickupItem(InventoryPickupItemEvent event) {
    }

    public static class InventoryCustomBlockHolder implements InventoryHolder {
        @Getter
        private final UUID worldUUID;
        @Getter
        private final int x, y, z;

        public InventoryCustomBlockHolder(InventoryCustomBlock customBlock) {
            this(customBlock.getSpawnedDisplay().getLocation());
        }

        public InventoryCustomBlockHolder(Location location) {
            this(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        public InventoryCustomBlockHolder(UUID worldUUID, int x, int y, int z) {
            this.worldUUID = worldUUID;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Location getCustomBlockLoc() {
            return new Location(Krimson.getInstance().getServer().getWorld(worldUUID), x, y, z);
        }

        public InventoryCustomBlock getCustomBlock() {
            return (InventoryCustomBlock) CustomBlockUtils.getCustomBlockFromLoc(getCustomBlockLoc());
        }

        @NotNull
        @Override
        public Inventory getInventory() {
            return getCustomBlock().getInventory();
        }
    }
}
