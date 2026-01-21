package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.persistence.PersistentDataType;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.PropertiesField;

public class LightBlock extends CustomBlock {
    @Getter
    private final int baseEmittingLightLevel;
    @Getter
    private Block lightBlock;

    public PropertiesField<Integer> getEmittingLightLevelField() {
        return getProperties().getEmittingLightLevelField();
    }

    public LightBlock(NamespacedKey key, NamespacedKey dropIdentifier, int emittingLightLevel) {
        super(key, dropIdentifier, Material.SLIME_BLOCK);

        this.baseEmittingLightLevel = emittingLightLevel;
    }

    public LightBlock(Block block) {
        super(block);

        this.baseEmittingLightLevel = getEmittingLightLevelField().get();
    }

    @Override
    public LightCustomBlockProperties getProperties() {
        return (LightCustomBlockProperties) super.getProperties();
    }

    @Override
    protected CustomBlockProperties createProperties(Block block) {
        return new LightCustomBlockProperties(block, this);
    }

    @Override
    public void spawn(Location blockLoc) {
        super.spawn(blockLoc);
        // Properties loaded in super.spawn -> setDisplayAndProperties -> createProperties

        lightBlock = blockLoc.getBlock().getRelative(BlockFace.UP);
        spawnLight();
    }

    public void spawnLight() {
        // Spawn the light
        lightBlock.setType(Material.LIGHT);
        Light light = (Light) lightBlock.getBlockData();
        light.setLevel(getEmittingLightLevelField().get());
        lightBlock.setBlockData(light);
        lightBlock.getState().update();
    }

    @Override
    public void tickAsync() {
        super.tickAsync();

        if (block.getType() != this.blockMaterial) {
            Block aboveBlock = block.getRelative(BlockFace.UP);

            if (aboveBlock.getType() == lightBlock.getType()) {
                aboveBlock.setType(Material.AIR);
            }
        }
    }

}
