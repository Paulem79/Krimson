package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.inventories.InventoryData;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

@Getter
public class InventoryCustomBlockProperties extends CustomBlockProperties {
    private int inventorySize;
    private String inventoryTitle;
    private InventoryData inventoryData;
    private Inventory inventory;

    public InventoryCustomBlockProperties(Block block, InventoryCustomBlock customBlock) {
        super(block, customBlock);
        load(customBlock);
    }

    private void load(InventoryCustomBlock customBlock) {
        this.inventorySize = getContainer().getOrDefault(Keys.INVENTORY_SIZE, customBlock.getBaseInventorySize());
        getContainer().set(Keys.INVENTORY_SIZE, this.inventorySize);

        this.inventoryTitle = getContainer().getOrDefault(Keys.INVENTORY_TITLE, customBlock.getBaseInventoryTitle());
        getContainer().set(Keys.INVENTORY_TITLE, this.inventoryTitle);

        if (getContainer().has(Keys.INVENTORY_DATA)) {
            this.inventoryData = getContainer().get(Keys.INVENTORY_DATA).orElseThrow();
            this.inventory = this.inventoryData.inventory();
        } else {
            this.inventory = KrimsonPlugin.getInstance().getServer().createInventory(
                    new InventoryCustomBlock.InventoryCustomBlockHolder(customBlock),
                    this.inventorySize,
                    this.inventoryTitle
            );
            this.inventoryData = new InventoryData(this.inventory, this.inventoryTitle);
            getContainer().set(Keys.INVENTORY_DATA, this.inventoryData);
        }
    }

    public void updateInventory(Inventory inventory) {
        this.inventory = inventory;
        this.inventoryData = new InventoryData(this.inventory, this.inventoryTitle);
        getContainer().set(Keys.INVENTORY_DATA, this.inventoryData);
    }
}