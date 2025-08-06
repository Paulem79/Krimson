package ovh.paulem.krimson.blocks.custom;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.persistence.PersistentDataType;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.properties.PropertiesField;

public class LightBlock extends CustomBlock {
    private final int baseEmittingLightLevel;
    @Getter
    protected PropertiesField<Integer> emittingLightLevel;
    @Getter
    private Block lightBlock;

    public LightBlock(NamespacedKey key, NamespacedKey dropIdentifier, int emittingLightLevel) {
        super(key, dropIdentifier, Material.SLIME_BLOCK);

        this.baseEmittingLightLevel = emittingLightLevel;
    }

    public LightBlock(Block block) {
        super(block);

        this.emittingLightLevel = new PropertiesField<>(Keys.EMITTING_LIGHT_LEVEL, properties, PersistentDataType.INTEGER);
        this.baseEmittingLightLevel = this.emittingLightLevel.get();
    }

    @Override
    public void spawn(Location blockLoc) {
        super.spawn(blockLoc);

        this.emittingLightLevel = new PropertiesField<>(Keys.EMITTING_LIGHT_LEVEL, this.baseEmittingLightLevel);
        properties.set(this.emittingLightLevel);

        lightBlock = blockLoc.getBlock().getRelative(BlockFace.UP);
        spawnLight();
    }

    public void spawnLight() {
        // Spawn the light
        lightBlock.setType(Material.LIGHT);
        Light light = (Light) lightBlock.getBlockData();
        light.setLevel(this.emittingLightLevel.get());
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
