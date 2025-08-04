package ovh.paulem.krimson.codec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import ovh.paulem.krimson.compat.CompatAccess;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * The Codec interface defines methods for encoding and decoding objects between two types, T and R.
 * It provides default implementations for handling byte array-based encoding and decoding.
 * Subclasses or implementing classes can provide specific implementations for the {@code encode} and {@code decode} methods.
 *
 * @param <T> the type of the original object to be encoded/decoded
 * @param <R> the type of the resulting encoded/decoded representation
 */
public interface Codec<T, R> {
    default R encode(T object) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            OutputStreamHandler<?> dataOutput = CompatAccess.getHandler(outputStream);

            return encode(dataOutput, object);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    /**
     * Encodes an object into the specified output.
     *
     * @param dataOutput the output to encode the object into
     * @param object     the object to encode
     * @throws Exception if an error occurs during encoding
     */
    R encode(@NotNull OutputStreamHandler<?> dataOutput, T object) throws Exception;

    default T decode(R object) {
        if (object instanceof byte[] datas) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(datas)) {
                InputStreamHandler<?> dataOutput = CompatAccess.getHandler(inputStream);

                return decode(dataOutput, object);
            } catch (Exception e) {
                throw new RuntimeException("Unable to decode class type.", e);
            }
        }

        throw new IllegalArgumentException("The given object is not a byte array.");
    }

    default T decode(InputStreamHandler<?> dataOutput) {
        try {
            return decode(dataOutput, null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decode class type.", e);
        }
    }

    /**
     * Decodes an object from the specified input.
     *
     * @param dataInput the input to decode the object from
     * @return the decoded object
     * @throws Exception if an error occurs during decoding
     */
    T decode(@NotNull InputStreamHandler<?> dataInput, @Nullable R object) throws Exception;
}
