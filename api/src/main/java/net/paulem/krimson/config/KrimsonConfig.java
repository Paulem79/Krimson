package net.paulem.krimson.config;

import lombok.Getter;
import net.paulem.arcana.config.Config;
import net.paulem.arcana.config.ConfigData;
import net.paulem.arcana.config.ConfigEntry;

@Config
public class KrimsonConfig implements ConfigData {
    /**
     * Renders item display custom blocks as 6 flat displays, one per face, each lit by the block laid
     * against it. When disabled, a single display is used, lit by the brightest neighbour block.
     */
    @Getter
    @ConfigEntry
    private boolean preciseLightning = true;

    @Getter
    @ConfigEntry
    private int viewDistance = 6;

    @Getter
    @ConfigEntry
    private boolean customMining = true;

    @Getter
    @ConfigEntry
    private double miningDamageRadius = 16;
}
