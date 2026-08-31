package net.paulem.krimson.blocks.mining;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Reimplementation of the vanilla mining formula, driving the server side mining of custom blocks.
 *
 * <p>The result is a per-tick damage: mining is finished once the accumulated damage reaches {@code 1}.</p>
 */
public final class MiningSpeedCalculator {
    private MiningSpeedCalculator() {
        /* This utility class should not be instantiated */
    }

    /**
     * Fraction of the block mined per tick by this player.
     *
     * @param player            the mining player
     * @param properties        the block's mining properties, must not {@link MiningProperties#inherits()}
     * @param carrierMaterial   the vanilla material carrying the custom block, used as hardness fallback
     * @param fatigueAmplifier  amplifier of the player's <em>own</em> mining fatigue, or {@code -1} when they
     *                          have none. The fatigue Krimson injects to freeze the client must never be
     *                          passed here, or mining would grind to a halt.
     * @return the damage dealt this tick, in {@code [0, 1]}
     */
    public static float damagePerTick(Player player, MiningProperties properties, Material carrierMaterial, int fatigueAmplifier) {
        float hardness = properties.inherits() ? carrierMaterial.getHardness() : properties.hardness();
        if (hardness <= 0) {
            return 1f;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        float speed = toolSpeed(tool, properties);

        int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
        if (efficiency > 0 && isCorrectTool(tool, properties)) {
            speed += (float) (efficiency * efficiency) + 1f;
        }

        PotionEffect haste = player.getPotionEffect(PotionEffectType.HASTE);
        if (haste != null) {
            speed *= 1f + 0.2f * (haste.getAmplifier() + 1);
        }

        if (fatigueAmplifier >= 0) {
            speed *= (float) Math.pow(0.3, Math.min(fatigueAmplifier + 1, 4));
        }

        if (player.isInWater() && !hasAquaAffinity(player)) {
            speed /= 5f;
        }

        if (!player.isOnGround()) {
            speed /= 5f;
        }

        float damage = speed / hardness;
        return damage / (canHarvest(tool, properties) ? 30f : 100f);
    }

    /**
     * Whether the held tool is of the family the block asks for. Blocks asking for {@link ToolType#NONE}
     * accept anything.
     */
    public static boolean isCorrectTool(ItemStack tool, MiningProperties properties) {
        return properties.requiredTool() == ToolType.NONE || ToolType.of(tool) == properties.requiredTool();
    }

    /**
     * Whether the held tool is good enough for the block to drop, mirroring vanilla's "correct tool for
     * drops" rule.
     */
    public static boolean canHarvest(ItemStack tool, MiningProperties properties) {
        if (!properties.requiresCorrectToolForDrops() || properties.requiredTool() == ToolType.NONE) {
            return true;
        }

        return ToolType.of(tool) == properties.requiredTool()
                && ToolTier.of(tool).getLevel() >= properties.requiredTier().getLevel();
    }

    private static float toolSpeed(ItemStack tool, MiningProperties properties) {
        if (properties.requiredTool() == ToolType.NONE || ToolType.of(tool) != properties.requiredTool()) {
            return 1f;
        }

        return ToolTier.of(tool).getSpeed();
    }

    private static boolean hasAquaAffinity(Player player) {
        EntityEquipment equipment = player.getEquipment();
        ItemStack helmet = equipment == null ? null : equipment.getHelmet();

        return helmet != null && helmet.getEnchantmentLevel(Enchantment.AQUA_AFFINITY) > 0;
    }
}
