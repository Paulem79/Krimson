package ovh.paulem.krimson.paper.versioned.serialize;

import org.bukkit.inventory.ItemStack;
import ovh.paulem.krimson.common.KrimsonPlugin;
import ovh.paulem.krimson.common.versioned.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.common.versioned.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.versioned.stream.output.OutputStreamHandler;

public class PaperItemSerializer extends ItemSerializerHandler {
    public PaperItemSerializer() {
        if(!messageSent) {
            KrimsonPlugin.getInstance().getLogger().info("Hooray! Using Paper's ItemStack (de)serialization, which gives you much better performance than Bukkit's!");
            messageSent = true;
        }
    }

    @Override
    public void serializeAndWrite(ItemStack stack, OutputStreamHandler<?> outputStream) throws Exception {
        /*byte[] serialized = (byte[]) ItemStack.class.getMethod("serializeAsBytes")
                .invoke(stack);*/

        byte[] serialized = stack.serializeAsBytes();

        outputStream.writeInt(serialized.length);
        outputStream.write(serialized);
    }

    @Override
    public ItemStack readAndDeserialize(InputStreamHandler<?> inputStream, int length) throws Exception {
        byte[] itemBytes = new byte[length];
        inputStream.read(itemBytes);

        return ItemStack.deserializeBytes(itemBytes);
        /*return (ItemStack) ItemStack.class.getMethod("deserializeBytes", byte[].class)
                .invoke(null, itemBytes);*/
    }
}
