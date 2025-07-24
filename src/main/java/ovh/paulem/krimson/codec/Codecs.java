package ovh.paulem.krimson.codec;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ovh.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import ovh.paulem.krimson.compat.CompatAccess;

public interface Codecs {
    ZLibCodec<ItemStack> ITEM_STACK_CODEC = new ZLibCodec<>() {
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
        public ItemStack decode(@NotNull InputStreamHandler<?> dataInput, byte[] object) throws Exception {
            int length = dataInput.readInt();

            if(length == 0) return null;

            ItemSerializerHandler itemSerializer = CompatAccess.getHandler();
            return itemSerializer.readAndDeserialize(dataInput, length);
        }
    };

    Codec<ItemStack, byte[]> ITEM_STACK_BASE_CODEC = ITEM_STACK_CODEC.toBasicCodec();

}
