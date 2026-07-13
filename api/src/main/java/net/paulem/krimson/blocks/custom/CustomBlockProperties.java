package net.paulem.krimson.blocks.custom;

import com.mojang.serialization.JsonOps;
import lombok.Getter;
import net.paulem.krimson.codec.Codecs;
import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.Properties;
import net.paulem.krimson.properties.PropertiesField;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;

public class CustomBlockProperties extends Properties {
    @Getter
    private PropertiesField<String> dropIdentifierField;
    @Getter
    private PropertiesField<String> blockMaterialField;
    @Getter
    private PropertiesField<String> displayedItemField;

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
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, getContainer(), PersistentDataType.STRING);
        } else {
            String encoded = Codecs.ITEM_STACK.encodeStart(JsonOps.INSTANCE, customBlock.getItemDisplayStack())
                    .resultOrPartial(error -> KrimsonPlugin.getInstance().getLogger().severe(error))
                    .orElseThrow(() -> new RuntimeException("Failed to encode Item Display Stack"))
                    .getAsString();
            this.displayedItemField = new PropertiesField<>(Keys.DISPLAYED_ITEM_KEY, encoded);
            getContainer().set(displayedItemField);
        }
    }
}
