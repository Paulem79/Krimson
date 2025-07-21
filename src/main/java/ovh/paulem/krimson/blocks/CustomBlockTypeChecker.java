package ovh.paulem.krimson.blocks;

import org.bukkit.entity.ItemDisplay;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.utils.properties.PropertiesStore;

public class CustomBlockTypeChecker {
    private final ItemDisplay itemDisplay;
    private final PropertiesStore properties;

    public CustomBlockTypeChecker(ItemDisplay itemDisplay) {
        this.itemDisplay = itemDisplay;
        this.properties = new PropertiesStore(itemDisplay);
    }

    public boolean isCustomBlock() {
        return properties.has(Keys.CUSTOM_BLOCK_KEY) &&
               properties.has(Keys.BLOCK_INSIDE_KEY) &&
               properties.has(Keys.DISPLAYED_ITEM_KEY);
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
        if (isCustomBlock()) {
            if (isLightBlock()) {
                return new LightBlock(itemDisplay);
            } else if (isInventoryBlock()) {
                return new InventoryCustomBlock(itemDisplay);
            }
            return new CustomBlock(itemDisplay);
        }

        return null;
    }
}
