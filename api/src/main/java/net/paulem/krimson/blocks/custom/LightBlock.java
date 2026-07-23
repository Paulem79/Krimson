package net.paulem.krimson.blocks.custom;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;

public class LightBlock extends CustomBlock {
    @Getter
    private final int baseEmittingLightLevel;
    @Getter
    private Block lightBlock;

    public LightBlock(NamespacedKey key, NamespacedKey dropIdentifier, int emittingLightLevel) {
        super(key, dropIdentifier, Material.SLIME_BLOCK);

        this.baseEmittingLightLevel = emittingLightLevel;
    }

    public LightBlock(Block block) {
        super(block);

        this.baseEmittingLightLevel = getProperties().getEmittingLightLevel();
        this.lightBlock = block.getRelative(BlockFace.UP);
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

        // Accès direct et propre à la donnée
        light.setLevel(getProperties().getEmittingLightLevel());

        lightBlock.setBlockData(light);
        lightBlock.getState().update();
    }

    @Override
    public void tickAsync() {
        super.tickAsync();

        if (block.getType() != this.blockMaterial) {
            Block aboveBlock = block.getRelative(BlockFace.UP);

            // Vérification de sécurité pour éviter le NullPointerException potentiel si lightBlock n'est pas instancié
            if (lightBlock != null && aboveBlock.getType() == lightBlock.getType()) {
                aboveBlock.setType(Material.AIR);
            }
        }
    }

    @Override
    public CustomBlock copyOf() {
        LightBlock copy = new LightBlock(this.getKey(), this.getDropIdentifier(), this.baseEmittingLightLevel);

        copy.registryReference = false;
        copy.setMeta(this.getMeta());

        return copy;
    }
}
