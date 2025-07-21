package ovh.paulem.krimson.utils;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.function.Function;

public class BlockUtils {
    public static byte computeLight(Function<Block, Byte> function, Block block) {
        byte maxLight = 0;
        for (BlockFace blockFace : BlockFace.values()) {
            if(!blockFace.isCartesian()) {
                continue;
            }

            byte actualLight = function.apply(block.getRelative(blockFace));
            if(actualLight > maxLight) {
                maxLight = actualLight;
            }
        }

        return maxLight;
    }
}
