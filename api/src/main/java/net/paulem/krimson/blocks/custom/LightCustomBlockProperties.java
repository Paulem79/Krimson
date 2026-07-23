package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import net.paulem.krimson.constants.Keys;
import org.bukkit.block.Block;

@Getter
public class LightCustomBlockProperties extends CustomBlockProperties {
    private int emittingLightLevel;

    public LightCustomBlockProperties(Block block, LightBlock customBlock) {
        super(block, customBlock);
        load(customBlock);
    }

    private void load(LightBlock customBlock) {
        this.emittingLightLevel = getContainer().getOrDefault(Keys.EMITTING_LIGHT_LEVEL, customBlock.getBaseEmittingLightLevel());
        getContainer().set(Keys.EMITTING_LIGHT_LEVEL, this.emittingLightLevel);
    }
}