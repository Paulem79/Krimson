package net.paulem.krimson.utils;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class BlockUtils {
    /**
     * Computes the maximum light level around a block in all Cartesian directions
     */
    public static byte computeLight(Function<Block, Byte> function, Block block) {
        byte maxLight = 0;
        for (BlockFace blockFace : BlockFace.values()) {
            if (!blockFace.isCartesian()) {
                continue;
            }

            byte actualLight = function.apply(block.getRelative(blockFace));
            if (actualLight > maxLight) {
                maxLight = actualLight;
            }
        }

        return maxLight;
    }

    public static boolean canPlaceOn(Player player, Block target) {
        return target.getWorld().getNearbyEntities(target.getLocation().add(.5, .5, .5), .5, .5, .5).stream().noneMatch(entity -> entity instanceof LivingEntity);
    }
}
