package ovh.paulem.krimson.utils;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class BlockUtils {
    /**
     * Computes the maximum light level around a block, if all cartesian faces are obstructed,
     * it will compute the light level on non-cartesian faces to avoid dark light glitch before
     * update when a cartesian face is being broken
     */
    public static byte computeLight(Function<Block, Byte> function, Block block) {
        byte computed = computeLight(function, block, false);

        // If the computed light is 0, it means all cartesian faces are obstructed
        if (computed == 0) {
            computed = computeLight(function, block, true);
        }

        return computed;
    }

    public static byte computeLight(Function<Block, Byte> function, Block block, boolean allFaces) {
        byte maxLight = 0;
        for (BlockFace blockFace : BlockFace.values()) {
            if (!allFaces && !blockFace.isCartesian()) {
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
