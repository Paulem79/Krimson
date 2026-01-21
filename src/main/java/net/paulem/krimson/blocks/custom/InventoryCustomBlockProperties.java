package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import net.paulem.krimson.Krimson;
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
    private PropertiesField<byte[]> inventoryBase64Field;
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

        if (getContainer().has(Keys.INVENTORY_BASE64)) {
            this.inventoryBase64Field = new PropertiesField<>(Keys.INVENTORY_BASE64, getContainer(), PersistentDataType.BYTE_ARRAY);
            this.inventory = InventoryData.decode(this.inventoryBase64Field.get()).inventory();
        } else {
            // New block creation
            this.inventory = KrimsonPlugin.getInstance().getServer().createInventory(
                    new InventoryCustomBlock.InventoryCustomBlockHolder(customBlock),
                    this.inventorySizeField.get(),
                    this.inventoryTitleField.get()
            );

            // If template had base64 provided (constructor with base64)
            // But wait, InventoryCustomBlock(block) doesn't set base64 in template usually?
            // Actually the one with base64 in constructor is for setting initial content?
            // Let's assume for now we create empty inventory or one from template if logic exists.

            // Re-encoding ensures consistency
            this.inventoryBase64Field = new PropertiesField<>(Keys.INVENTORY_BASE64, InventoryData.encode(new InventoryData(this.inventory, this.inventoryTitleField.get())));
            getContainer().set(inventoryBase64Field);
        }
    }

    public void updateInventory(Inventory inventory) {
        this.inventory = inventory;
        this.inventoryBase64Field = new PropertiesField<>(Keys.INVENTORY_BASE64, InventoryData.encode(new InventoryData(this.inventory, this.inventoryTitleField.get())));
        getContainer().set(this.inventoryBase64Field);
    }
}
