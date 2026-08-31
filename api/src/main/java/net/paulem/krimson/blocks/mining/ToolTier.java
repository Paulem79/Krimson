package net.paulem.krimson.blocks.mining;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Tool material tier, carrying the vanilla mining speed and harvest level.
 *
 * <p>Gold is fast but has the harvest level of wood, exactly like vanilla.</p>
 */
@Getter
public enum ToolTier {
    NONE(1.0f, 0),
    WOOD(2.0f, 0),
    GOLD(12.0f, 0),
    STONE(4.0f, 1),
    IRON(6.0f, 2),
    DIAMOND(8.0f, 3),
    NETHERITE(9.0f, 4);

    /** Base mining speed of the tier, as used by the vanilla mining formula. */
    private final float speed;
    /** Harvest level: a tool may harvest a block whose required level is lower or equal. */
    private final int level;

    ToolTier(float speed, int level) {
        this.speed = speed;
        this.level = level;
    }

    /**
     * Resolves the tier of the given stack, {@link #NONE} when the item is not a tiered tool.
     */
    public static ToolTier of(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return NONE;
        }

        Material material = stack.getType();
        String name = material.name();

        if (name.startsWith("WOODEN_")) return WOOD;
        if (name.startsWith("GOLDEN_")) return GOLD;
        if (name.startsWith("STONE_")) return STONE;
        if (name.startsWith("IRON_")) return IRON;
        if (name.startsWith("DIAMOND_")) return DIAMOND;
        if (name.startsWith("NETHERITE_")) return NETHERITE;
        if (material == Material.SHEARS) return IRON;

        return NONE;
    }
}
