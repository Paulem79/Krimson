package net.paulem.krimson.blocks.mining;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The state of one player mining one custom block, ticked by {@link MiningManager}.
 */
@Getter
public class MiningSession {
    private final Block block;
    private final NamespacedKey blockKey;
    private final MiningProperties properties;
    private final Material carrierMaterial;

    /**
     * The player's own mining fatigue amplifier when the session started, {@code -1} when they had none.
     * Kept so the effect can be restored afterwards, and so the injected fatigue never skews the speed.
     */
    private final int ownFatigueAmplifier;
    private final int ownFatigueDuration;

    /** Accumulated progress, the block breaks once it reaches {@code 1}. */
    @Setter
    private float progress;
    @Setter
    private float damagePerTick;
    /** Held item the current {@link #damagePerTick} was computed for, to detect a tool swap. */
    @Setter
    @Nullable
    private ItemStack tool;
    @Setter
    private int ticks;

    public MiningSession(Block block, NamespacedKey blockKey, MiningProperties properties, Material carrierMaterial,
                         int ownFatigueAmplifier, int ownFatigueDuration) {
        this.block = block;
        this.blockKey = blockKey;
        this.properties = properties;
        this.carrierMaterial = carrierMaterial;
        this.ownFatigueAmplifier = ownFatigueAmplifier;
        this.ownFatigueDuration = ownFatigueDuration;
    }

    public boolean hasOwnFatigue() {
        return ownFatigueAmplifier >= 0;
    }
}
