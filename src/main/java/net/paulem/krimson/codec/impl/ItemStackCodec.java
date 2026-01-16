package net.paulem.krimson.codec.impl;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.codec.ZLibCodec;
import net.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import net.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import net.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import net.paulem.krimson.compat.CompatAccess;

public class ItemStackCodec extends ZLibCodec<ItemStack> {
    @Override
    protected OutputStreamHandler<?> createEncoder(@NotNull OutputStreamHandler<?> dataOutput, ItemStack object) throws Exception {
        if (object == null) {
            // Ensure the correct order by including empty/null items
            // Simply remove the write line if you don't want this
            dataOutput.writeInt(0);
            return dataOutput;
        }

        ItemSerializerHandler itemSerializer = CompatAccess.getHandler();
        itemSerializer.serializeAndWrite(object, dataOutput);
        return dataOutput;
    }

    @Override
    public ItemStack decode(@NotNull InputStreamHandler<?> dataInput, byte @Nullable [] object) throws Exception {
        int length = dataInput.readInt();

        if (length == 0) return null;

        ItemSerializerHandler itemSerializer = CompatAccess.getHandler();
        return itemSerializer.readAndDeserialize(dataInput, length);
    }
}
