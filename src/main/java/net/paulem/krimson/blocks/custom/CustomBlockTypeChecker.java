package net.paulem.krimson.blocks.custom;

import org.bukkit.block.Block;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.PDCWrapper;

public class CustomBlockTypeChecker {
    private final Block block;
    private final PDCWrapper properties;

    public CustomBlockTypeChecker(Block block) {
        this.block = block;
        this.properties = new PDCWrapper(block);
    }

    public boolean isLightBlock() {
        return properties.has(Keys.EMITTING_LIGHT_LEVEL);
    }

    public boolean isInventoryBlock() {
        return properties.has(Keys.INVENTORY_SIZE) &&
                properties.has(Keys.INVENTORY_TITLE) &&
                properties.has(Keys.INVENTORY_BASE64);
    }

    public CustomBlock get() {
        if (Krimson.isCustomBlock(block)) {
            if (isLightBlock()) {
                return new LightBlock(block);
            } else if (isInventoryBlock()) {
                return new InventoryCustomBlock(block);
            }
            return new CustomBlock(block);
        }

        return null;
    }
}
