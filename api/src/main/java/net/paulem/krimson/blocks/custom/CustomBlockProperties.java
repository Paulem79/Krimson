package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import net.paulem.krimson.codec.pdc.ItemStackDataType;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.Properties;
import net.paulem.krimson.properties.PropertiesField;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class CustomBlockProperties extends Properties {
    @Getter
    private PropertiesField<String> dropIdentifierField;
    @Getter
    private PropertiesField<String> blockMaterialField;
    @Getter
    private PropertiesField<ItemStack> displayedItemField;

    public CustomBlockProperties(Block block, CustomBlock customBlock) {
        super(block);
        load(customBlock);
    }

    private void load(CustomBlock customBlock) {
        getContainer().set(Keys.IDENTIFIER_KEY, customBlock.getKey().toString());

        if (getContainer().has(Keys.DROP_IDENTIFIER_KEY)) {
            this.dropIdentifierField = new PropertiesField<>(Keys.DROP_IDENTIFIER_KEY, getContainer(), PersistentDataType.STRING);
        } else {
            this.dropIdentifierField = new PropertiesField<>(Keys.DROP_IDENTIFIER_KEY, customBlock.getDropIdentifier().toString());
            getContainer().set(dropIdentifierField);
        }

        if (getContainer().has(Keys.BLOCK_INSIDE_KEY)) {
            this.blockMaterialField = new PropertiesField<>(Keys.BLOCK_INSIDE_KEY, getContainer(), PersistentDataType.STRING);
        } else {
            this.blockMaterialField = new PropertiesField<>(Keys.BLOCK_INSIDE_KEY, customBlock.getBlockMaterial().name());
            getContainer().set(blockMaterialField);
        }

        if (getContainer().has(Keys.DISPLAYED_ITEM_KEY)) {
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, getContainer(), ItemStackDataType.INSTANCE);
        } else {
            ItemStack stack = customBlock.getItemDisplayStack();
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, stack);
            getContainer().set(displayedItemField);
        }
    }
}
