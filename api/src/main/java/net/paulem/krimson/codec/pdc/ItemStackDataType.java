package net.paulem.krimson.codec.pdc;

import net.paulem.krimson.paper.compat.serialize.PaperItemSerializer;
import net.paulem.krimson.paper.compat.stream.input.PaperInputStream;
import net.paulem.krimson.paper.compat.stream.output.PaperOutputStream;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ItemStackDataType implements PersistentDataType<byte[], ItemStack> {

    public static final ItemStackDataType INSTANCE = new ItemStackDataType();

    @Override
    public @NonNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NonNull Class<ItemStack> getComplexType() {
        return ItemStack.class;
    }

    @Override
    public byte @NonNull [] toPrimitive(@NonNull ItemStack complex, @NonNull PersistentDataAdapterContext context) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PaperOutputStream handler = new PaperOutputStream(outputStream);

            // Added complex.isEmpty() to ignore air
            if (complex.isEmpty()) {
                handler.writeInt(0);
            } else {
                PaperItemSerializer.INSTANCE.serializeAndWrite(complex, handler);
            }

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ItemStack", e);
        }
    }

    @Override
    public @NonNull ItemStack fromPrimitive(byte @NonNull [] primitive, @NonNull PersistentDataAdapterContext context) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(primitive);
            PaperInputStream handler = new PaperInputStream(inputStream);

            int length = handler.readInt();
            if (length == 0) return new ItemStack(Material.AIR);

            return PaperItemSerializer.INSTANCE.readAndDeserialize(handler, length);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ItemStack", e);
        }
    }
}