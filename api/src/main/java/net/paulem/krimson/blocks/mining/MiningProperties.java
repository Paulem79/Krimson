package net.paulem.krimson.blocks.mining;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

/**
 * Describes how long a custom block takes to mine, which tool it wants and which sounds it makes.
 *
 * <p>The default of every custom block is {@link #INHERIT}: the block keeps behaving exactly like the
 * vanilla material carrying it, and no server side mining takes place. Give a block real properties on its
 * registry template (before {@code Blocks.REGISTRY.freeze()}) to opt in:</p>
 *
 * <pre>{@code
 * PluginBlocks.NOTE_TEST.setMiningProperties(MiningProperties.pickaxe(15f, ToolTier.IRON));
 * }</pre>
 *
 * @param hardness                   vanilla-style hardness (stone is {@code 1.5f}); {@code 0} breaks
 *                                   instantly, {@link #INHERIT_HARDNESS} keeps the vanilla behaviour
 * @param requiredTool               tool family granting the speed bonus, {@link ToolType#NONE} for any
 * @param requiredTier               minimum tier able to harvest the block
 * @param requiresCorrectToolForDrops whether a wrong tool means no drop, like vanilla ores
 * @param breakSound                 sound played once the block breaks; a {@code Sounds} registry key when
 *                                   it names a custom sound, otherwise a plain vanilla sound key
 * @param digSound                   sound played every few ticks while mining, same resolution rules
 */
public record MiningProperties(
        float hardness,
        ToolType requiredTool,
        ToolTier requiredTier,
        boolean requiresCorrectToolForDrops,
        @Nullable NamespacedKey breakSound,
        @Nullable NamespacedKey digSound
) {
    /** Sentinel hardness meaning "use the carrier material's own hardness and vanilla mining". */
    public static final float INHERIT_HARDNESS = -1f;

    /** Default properties: the block mines exactly like the vanilla material carrying it. */
    public static final MiningProperties INHERIT = new MiningProperties(
            INHERIT_HARDNESS, ToolType.NONE, ToolTier.NONE, false, null, null
    );

    /**
     * Whether these properties leave the block to vanilla mining.
     */
    public boolean inherits() {
        return hardness < 0;
    }

    /**
     * Properties with the given hardness, harvestable by hand and by any tool.
     */
    public static MiningProperties of(float hardness) {
        return new MiningProperties(hardness, ToolType.NONE, ToolTier.NONE, false, null, null);
    }

    /**
     * Properties for an ore-like block: a pickaxe of at least {@code tier} is required to get the drop.
     */
    public static MiningProperties pickaxe(float hardness, ToolTier tier) {
        return tool(hardness, ToolType.PICKAXE, tier);
    }

    /**
     * Properties requiring the given tool family and tier to get the drop.
     */
    public static MiningProperties tool(float hardness, ToolType type, ToolTier tier) {
        return new MiningProperties(hardness, type, tier, true, null, null);
    }

    /**
     * Copy of these properties with the given break sound.
     */
    public MiningProperties withBreakSound(@Nullable NamespacedKey sound) {
        return new MiningProperties(hardness, requiredTool, requiredTier, requiresCorrectToolForDrops, sound, digSound);
    }

    /**
     * Copy of these properties with the given digging sound.
     */
    public MiningProperties withDigSound(@Nullable NamespacedKey sound) {
        return new MiningProperties(hardness, requiredTool, requiredTier, requiresCorrectToolForDrops, breakSound, sound);
    }
}
