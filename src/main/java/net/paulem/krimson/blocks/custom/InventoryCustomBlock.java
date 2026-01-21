package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import net.paulem.krimson.common.KrimsonPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.inventories.InventoryData;
import net.paulem.krimson.inventories.InventoryDiff;
import net.paulem.krimson.properties.PropertiesField;
import net.paulem.krimson.utils.CustomBlockUtils;

import java.util.UUID;

// FIXME: there are still some difficulties with this approach, especially around parsing/saving it asynchronously which can become an issue if there are a lot of these and they have large contents, but for a basic approach this will be fine https://discord.com/channels/690411863766466590/741875863271899136/1397175434432614472 (saving in files, with pointers in PDC can be better)
public class InventoryCustomBlock extends CustomBlock {
    @Getter
    private final int baseInventorySize;
    @Getter
    private final String baseInventoryTitle;
    private final InventoryDiff inventoryDiff = new InventoryDiff();

    public PropertiesField<Integer> getInventorySizeField() {
        return getProperties().getInventorySizeField();
    }

    public PropertiesField<String> getInventoryTitleField() {
        return getProperties().getInventoryTitleField();
    }

    public PropertiesField<byte[]> getInventoryBase64Field() {
        return getProperties().getInventoryBase64Field();
    }

    public Inventory getInventory() {
        return getProperties().getInventory();
    }

    public InventoryCustomBlock(NamespacedKey key, NamespacedKey dropIdentifier, Material blockInside, int inventorySize, String inventoryTitle, byte[] inventoryBase64) {
        this(key, dropIdentifier, blockInside, inventorySize, inventoryTitle);
        // Note: inventoryBase64 in constructor is currently not used for instantiation of property directly here
        // It would require logic in InventoryCustomBlockProperties to accept an optional initial base64
        // For now adhering to request to migrate system.
    }

    public InventoryCustomBlock(NamespacedKey key, NamespacedKey dropIdentifier, Material blockInside, int inventorySize, String inventoryTitle) {
        super(key, dropIdentifier, blockInside);

        this.baseInventorySize = inventorySize;
        this.baseInventoryTitle = inventoryTitle;
    }

    public InventoryCustomBlock(Block block) {
        super(block);

        // Fields accessed from properties now
        // But we need to fill base fields for the "registry" contract if needed,
        // though usually this constructor creates a LIVE block, which uses properties.
        // base fields are final so they need to be set.
        // We can read them from the properties we just loaded in super(block).

        this.baseInventorySize = getInventorySizeField().get();
        this.baseInventoryTitle = getInventoryTitleField().get();
    }

    @Override
    public InventoryCustomBlockProperties getProperties() {
        return (InventoryCustomBlockProperties) super.getProperties();
    }

    @Override
    protected CustomBlockProperties createProperties(Block block) {
        return new InventoryCustomBlockProperties(block, this);
    }

    @Override
    public void spawn(Location blockLoc) {
        super.spawn(blockLoc);
        // Properties and inventory creation is now handled in InventoryCustomBlockProperties
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        player.openInventory(getInventory());

        event.setCancelled(true);
    }

    @Override
    public void onUnload() {
        super.onUnload();

        Inventory inv = getInventory();
        if (inv != null) {
            this.inventoryDiff.setNow(inv.getContents());
            if (inventoryDiff.hasChanges()) {
                 getProperties().updateInventory(inv);
            }

            this.inventoryDiff.setBefore(inv.getContents());
        }
    }

    public void onGuiOpen(InventoryOpenEvent event) {
        // Intentionally empty
    }

    public void onGuiClose(InventoryCloseEvent event) {
        this.inventoryDiff.setNow(event.getInventory().getContents());

        if (inventoryDiff.hasChanges()) {
            getProperties().updateInventory(event.getInventory());
        }

        this.inventoryDiff.setBefore(event.getInventory().getContents());
    }

    public void onGuiClick(InventoryClickEvent event) {
        // Intentionally empty
    }

    public void onGuiDrag(InventoryDragEvent event) {
        // Intentionally empty
    }

    public void onGuiMoveItem(InventoryMoveItemEvent event) {
        // Intentionally empty
    }

    public void onGuiPickupItem(InventoryPickupItemEvent event) {
        // Intentionally empty
    }

    public record InventoryCustomBlockHolder(UUID worldUUID, int x, int y, int z) implements InventoryHolder {
        public InventoryCustomBlockHolder(InventoryCustomBlock customBlock) {
            this(customBlock.getBlock().getLocation());
        }

        public InventoryCustomBlockHolder(Location location) {
            this(location.getWorld() != null ? location.getWorld().getUID() : null, location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        public Location getCustomBlockLoc() {
            if (worldUUID == null) return null;
            return new Location(KrimsonPlugin.getInstance().getServer().getWorld(worldUUID), x, y, z);
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

    @Override
    public CustomBlock copyOf() {
        InventoryCustomBlock copy = new InventoryCustomBlock(this.getKey(), this.getDropIdentifier(), this.getBlockMaterial(), this.baseInventorySize, this.baseInventoryTitle);

        copy.registryReference = false;
        // copy.setMeta(this.getMeta()); // setMeta is protected in CustomBlock and Lombok generated?
        // CustomBlock has @Setter on meta? Yes but protected getter.
        // The previous code had copy.setMeta(this.getMeta()) so it should be fine if available.
        // Checking CustomBlock: @Setter @Getter(lombok.AccessLevel.PROTECTED) private Consumer<ItemMeta> meta;
        // The setter is public (default lombok behavior if not specified).
        copy.setMeta(this.getMeta());

        return copy;
    }
}
