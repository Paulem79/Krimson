package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.PropertiesField;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;

public class LightCustomBlockProperties extends CustomBlockProperties {
    @Getter
    private PropertiesField<Integer> emittingLightLevelField;

    public LightCustomBlockProperties(Block block, LightBlock customBlock) {
        super(block, customBlock);
        load(customBlock);
    }

    private void load(LightBlock customBlock) {
        if (getContainer().has(Keys.EMITTING_LIGHT_LEVEL)) {
            this.emittingLightLevelField = new PropertiesField<>(Keys.EMITTING_LIGHT_LEVEL, getContainer(), PersistentDataType.INTEGER);
        } else {
            this.emittingLightLevelField = new PropertiesField<>(Keys.EMITTING_LIGHT_LEVEL, customBlock.getBaseEmittingLightLevel());
            getContainer().set(emittingLightLevelField);
        }
    }
}
