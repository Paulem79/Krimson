package net.paulem.krimson.config;

import lombok.Getter;
import net.paulem.arcana.config.Config;
import net.paulem.arcana.config.ConfigData;
import net.paulem.arcana.config.ConfigEntry;

@Config
public class KrimsonConfig implements ConfigData {
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
