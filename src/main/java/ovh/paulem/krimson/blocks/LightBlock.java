package ovh.paulem.krimson.blocks;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.properties.PropertiesField;
import ovh.paulem.krimson.utils.CustomBlockUtils;

public class LightBlock extends CustomBlock {
    @Getter
    protected PropertiesField<Integer> emittingLightLevel;

    private final int baseEmittingLightLevel;

    @Getter
    private Block lightBlock;

    public LightBlock(NamespacedKey dropIdentifier, int emittingLightLevel) {
        super(dropIdentifier, Material.SLIME_BLOCK, new ItemStack(Material.AMETHYST_BLOCK));

        this.baseEmittingLightLevel = emittingLightLevel;
    }

    public LightBlock(ItemDisplay itemDisplay) {
        super(itemDisplay);

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
    public void tick() {
        super.tick();

        Block block = CustomBlockUtils.getBlockFromDisplay(this.spawnedDisplay);
        if (block.getType() != this.blockInside) {
            Block aboveBlock = block.getRelative(BlockFace.UP);

            if(aboveBlock.getType() == lightBlock.getType()) {
                aboveBlock.setType(Material.AIR);
            }
        }
    }
}
