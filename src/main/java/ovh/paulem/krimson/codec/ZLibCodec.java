package ovh.paulem.krimson.codec;

import org.jetbrains.annotations.NotNull;
import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import ovh.paulem.krimson.utils.ZLibUtils;
import ovh.paulem.krimson.compat.CompatAccess;

import java.io.ByteArrayInputStream;

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
            public T decode(@NotNull InputStreamHandler<?> dataInput, byte[] object) throws Exception {
                return ZLibCodec.this.decode(dataInput, object);
            }
        };
    }
}
