package ovh.paulem.krimson.codec;

import org.bukkit.inventory.ItemStack;
import ovh.paulem.krimson.codec.impl.ItemStackCodec;

public interface Codecs {
    ZLibCodec<ItemStack> ITEM_STACK_CODEC = new ItemStackCodec();
    Codec<ItemStack, byte[]> ITEM_STACK_BASE_CODEC = ITEM_STACK_CODEC.toBasicCodec();
}
