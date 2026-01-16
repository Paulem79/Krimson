package net.paulem.krimson.codec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import net.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import net.paulem.krimson.compat.CompatAccess;
import net.paulem.krimson.utils.ZLibUtils;

import java.io.ByteArrayInputStream;

/**
 * An abstract implementation of the {@link Codec} interface that utilizes the ZLIB compression algorithm
 * for serializing and deserializing objects. This class provides methods for encoding and decoding objects
 * and facilitates both compressed and uncompressed operations by converting the object and its byte array representation.
 *
 * @param <T> the object type to be encoded/decoded
 */
public abstract class ZLibCodec<T> implements Codec<T, byte[]> {
    protected abstract OutputStreamHandler<?> createEncoder(@NotNull OutputStreamHandler<?> dataOutput, T object) throws Exception;

    @Override
    public byte[] encode(@NotNull OutputStreamHandler<?> dataOutput, T object) throws Exception {
        return ZLibUtils.compress(createEncoder(dataOutput, object).toByteArray());
    }

    @Override
    public T decode(byte[] compressed) {
        try {
            byte[] decompressed = ZLibUtils.decompress(compressed);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(decompressed);
            InputStreamHandler<?> dataOutput = CompatAccess.getHandler(inputStream);

            return decode(dataOutput, decompressed);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decode class type.", e);
        }
    }

    /**
     * Converts the current {@link ZLibCodec} implementation into a basic codec that bypasses
     * the ZLIB compression layer, allowing for direct encoding and decoding of objects without compression.
     *
     * @return a {@link Codec} instance capable of encoding objects of type {@code T} to their raw byte array representation
     * and decoding them back to objects of type {@code T}, without applying any compression or decompression.
     */
    public Codec<T, byte[]> toBasicCodec() {
        return new Codec<>() {
            @Override
            public byte[] encode(@NotNull OutputStreamHandler<?> dataOutput, T object) throws Exception {
                return createEncoder(dataOutput, object).toByteArray();
            }

            @Override
            public T decode(byte[] object) {
                try {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(object);
                    InputStreamHandler<?> dataOutput = CompatAccess.getHandler(inputStream);

                    return decode(dataOutput, object);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to decode class type.", e);
                }
            }

            @Override
            public T decode(@NotNull InputStreamHandler<?> dataInput, byte @Nullable [] object) throws Exception {
                return ZLibCodec.this.decode(dataInput, object);
            }
        };
    }
}
