package net.paulem.krimson.codec;

import com.mojang.serialization.Codec;
import net.paulem.krimson.codec.dfu.ItemStackDFUCodec;
import net.paulem.krimson.codec.dfu.ItemStackDFUListCodec;
import org.bukkit.inventory.ItemStack;

/**
 * Central registry for codecs in Krimson.
 */
public final class Codecs {
    private Codecs() {
        // Prevent instantiation
    }

    // ItemStack codec
    public static final Codec<ItemStack> ITEM_STACK = ItemStackDFUCodec.CODEC;
    public static final Codec<ItemStack[]> ITEM_STACK_LIST = ItemStackDFUListCodec.CODEC;
}
