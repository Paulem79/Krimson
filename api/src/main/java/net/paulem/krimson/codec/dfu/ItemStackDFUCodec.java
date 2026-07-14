package net.paulem.krimson.codec.dfu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.paulem.krimson.paper.compat.serialize.PaperItemSerializer;
import net.paulem.krimson.paper.compat.stream.input.PaperInputStream;
import net.paulem.krimson.paper.compat.stream.output.PaperOutputStream;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * DataFixerUpper-compatible codec for Bukkit ItemStack.
 * This codec serializes ItemStack to a Base64 string representation.
 */
public class ItemStackDFUCodec implements PrimitiveCodec<ItemStack> {

    public static final Codec<ItemStack> CODEC = new ItemStackDFUCodec();

    @Override
    public <T> DataResult<ItemStack> read(final DynamicOps<T> ops, final T input) {
        return ops.getStringValue(input).flatMap(base64 -> {
            try {
                byte[] decoded = Base64.getDecoder().decode(base64);

                ByteArrayInputStream inputStream = new ByteArrayInputStream(decoded);
                PaperInputStream handler = new PaperInputStream(inputStream);

                int length = handler.readInt();
                if (length == 0) return DataResult.success(null);

                return DataResult.success(PaperItemSerializer.INSTANCE.readAndDeserialize(handler, length));
            } catch (Exception e) {
                return DataResult.error(() -> "Failed to decode ItemStack: " + e.getMessage());
            }
        });
    }

    @Override
    public <T> T write(final DynamicOps<T> ops, final ItemStack value) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PaperOutputStream handler = new PaperOutputStream(outputStream);

            if (value == null) {
                handler.writeInt(0);
            } else {
                PaperItemSerializer.INSTANCE.serializeAndWrite(value, handler);
            }

            byte[] bytes = handler.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return ops.createString(base64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode ItemStack", e);
        }
    }
}