package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import net.paulem.krimson.codec.pdc.InventoryDataType;
import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.inventories.InventoryData;
import net.paulem.krimson.properties.PropertiesField;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

public class InventoryCustomBlockProperties extends CustomBlockProperties {
    @Getter
    private PropertiesField<Integer> inventorySizeField;
    @Getter
    private PropertiesField<String> inventoryTitleField;
    @Getter
    private PropertiesField<InventoryData> inventoryDataField;
    @Getter
    private Inventory inventory;

    public InventoryCustomBlockProperties(Block block, InventoryCustomBlock customBlock) {
        super(block, customBlock);
        load(customBlock);
    }

    private void load(InventoryCustomBlock customBlock) {
        if (getContainer().has(Keys.INVENTORY_SIZE)) {
            this.inventorySizeField = new PropertiesField<>(Keys.INVENTORY_SIZE, getContainer(), PersistentDataType.INTEGER);
        } else {
            this.inventorySizeField = new PropertiesField<>(Keys.INVENTORY_SIZE, customBlock.getBaseInventorySize());
            getContainer().set(inventorySizeField);
        }

        if (getContainer().has(Keys.INVENTORY_TITLE)) {
            this.inventoryTitleField = new PropertiesField<>(Keys.INVENTORY_TITLE, getContainer(), PersistentDataType.STRING);
        } else {
            this.inventoryTitleField = new PropertiesField<>(Keys.INVENTORY_TITLE, customBlock.getBaseInventoryTitle());
            getContainer().set(inventoryTitleField);
        }

        if (getContainer().has(Keys.INVENTORY_DATA)) {
            this.inventoryDataField = new PropertiesField<>(Keys.INVENTORY_DATA, getContainer(), InventoryDataType.INSTANCE);
            this.inventory = this.inventoryDataField.get().inventory();
        } else {
            // New block creation
            this.inventory = KrimsonPlugin.getInstance().getServer().createInventory(
                    new InventoryCustomBlock.InventoryCustomBlockHolder(customBlock),
                    this.inventorySizeField.get(),
                    this.inventoryTitleField.get()
            );

            this.inventoryDataField = new PropertiesField<>(Keys.INVENTORY_DATA, new InventoryData(this.inventory, this.inventoryTitleField.get()));
            getContainer().set(inventoryDataField);
        }
    }

    public void updateInventory(Inventory inventory) {
        this.inventory = inventory;
        this.inventoryDataField = new PropertiesField<>(Keys.INVENTORY_DATA, new InventoryData(this.inventory, this.inventoryTitleField.get()));
        getContainer().set(this.inventoryDataField);
    }
}
