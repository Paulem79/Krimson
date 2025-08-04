package ovh.paulem.krimson.paper.compat.serialize;

import org.bukkit.inventory.ItemStack;
import ovh.paulem.krimson.common.KrimsonPlugin;
import ovh.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.compat.stream.output.OutputStreamHandler;

public class PaperItemSerializer extends ItemSerializerHandler {
    public PaperItemSerializer() {
        if (!messageSent) {
            KrimsonPlugin.getInstance().getLogger().info("Hooray! Using Paper's ItemStack (de)serialization, which gives you much better performance than Bukkit's!");
            messageSent = true;
        }
    }

    @Override
    public void serializeAndWrite(ItemStack stack, OutputStreamHandler<?> outputStream) throws Exception {
        byte[] serialized = stack.serializeAsBytes();

        outputStream.writeInt(serialized.length);
        outputStream.write(serialized);
    }

    @Override
    public ItemStack readAndDeserialize(InputStreamHandler<?> inputStream, int length) throws Exception {
        byte[] itemBytes = new byte[length];
        inputStream.read(itemBytes);

        return ItemStack.deserializeBytes(itemBytes);
    }
}
