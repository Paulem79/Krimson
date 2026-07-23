package net.paulem.krimson.blocks.custom;

import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.Properties;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class CustomBlockProperties extends Properties {
    private String dropIdentifier;
    private String blockMaterial;
    private ItemStack displayedItem;

    public CustomBlockProperties(Block block, CustomBlock customBlock) {
        super(block);
        load(customBlock);
    }

    private void load(CustomBlock customBlock) {
        getContainer().set(Keys.IDENTIFIER, customBlock.getKey().toString());

        this.dropIdentifier = getContainer().getOrDefault(Keys.DROP_IDENTIFIER, customBlock.getDropIdentifier().toString());
        getContainer().set(Keys.DROP_IDENTIFIER, this.dropIdentifier);

        this.blockMaterial = getContainer().getOrDefault(Keys.BLOCK_INSIDE, customBlock.getBlockMaterial().name());
        getContainer().set(Keys.BLOCK_INSIDE, this.blockMaterial);

        this.displayedItem = getContainer().getOrDefault(Keys.DISPLAYED_ITEM, customBlock.getItemDisplayStack());
        getContainer().set(Keys.DISPLAYED_ITEM, this.displayedItem);
    }
}
