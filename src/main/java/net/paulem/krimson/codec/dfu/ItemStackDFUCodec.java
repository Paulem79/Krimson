package net.paulem.krimson.codec.dfu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import net.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import net.paulem.krimson.compat.CompatAccess;
import net.paulem.krimson.utils.ZLibUtils;
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
                byte[] compressed = Base64.getDecoder().decode(base64);
                byte[] decompressed = ZLibUtils.decompress(compressed);

                ByteArrayInputStream inputStream = new ByteArrayInputStream(decompressed);
                InputStreamHandler<?> handler = CompatAccess.getHandler(inputStream);

                int length = handler.readInt();
                if (length == 0) return DataResult.success(null);

                return DataResult.success(CompatAccess.getHandler().readAndDeserialize(handler, length));
            } catch (Exception e) {
                return DataResult.error(() -> "Failed to decode ItemStack: " + e.getMessage());
            }
        });
    }

    @Override
    public <T> T write(final DynamicOps<T> ops, final ItemStack value) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamHandler<?> handler = CompatAccess.getHandler(outputStream);

            if (value == null) {
                handler.writeInt(0);
            } else {
                CompatAccess.getHandler().serializeAndWrite(value, handler);
            }

            byte[] bytes = handler.toByteArray();
            byte[] compressed = ZLibUtils.compress(bytes);
            String base64 = Base64.getEncoder().encodeToString(compressed);
            return ops.createString(base64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode ItemStack", e);
        }
    }
}