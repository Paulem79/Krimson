package net.paulem.krimson.blocks.mining;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The family of tool a {@link MiningProperties} can require.
 *
 * <p>Detection is done on the {@link Material} name instead of NMS tool tags: custom blocks only ever need
 * the vanilla tool families, and this keeps the API module free of version specific code.</p>
 */
public enum ToolType {
    /** No specific tool: the block is always harvestable and no tool speed bonus applies. */
    NONE,
    PICKAXE,
    AXE,
    SHOVEL,
    HOE,
    SHEARS,
    SWORD;

    /**
     * Resolves the tool family of the given stack, {@link #NONE} when the item is not a tool.
     */
    public static ToolType of(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return NONE;
        }

        String name = stack.getType().name();

        if (name.endsWith("_PICKAXE")) return PICKAXE;
        if (name.endsWith("_AXE")) return AXE;
        if (name.endsWith("_SHOVEL")) return SHOVEL;
        if (name.endsWith("_HOE")) return HOE;
        if (name.endsWith("_SWORD")) return SWORD;
        if (stack.getType() == Material.SHEARS) return SHEARS;

        return NONE;
    }
}
