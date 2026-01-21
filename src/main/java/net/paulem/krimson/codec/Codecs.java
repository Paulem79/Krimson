package net.paulem.krimson.codec;

import net.paulem.krimson.codec.dfu.ItemStackDFUCodec;
import org.bukkit.inventory.ItemStack;

/**
 * Central registry for codecs in Krimson.
 */
public final class Codecs {
    private Codecs() {
        // Prevent instantiation
    }

    // ItemStack codec
    public static final com.mojang.serialization.Codec<ItemStack> ITEM_STACK = ItemStackDFUCodec.CODEC;
}
