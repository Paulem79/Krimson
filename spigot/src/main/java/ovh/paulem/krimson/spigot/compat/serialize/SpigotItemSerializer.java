package ovh.paulem.krimson.spigot.compat.serialize;

import org.bukkit.inventory.ItemStack;
import ovh.paulem.krimson.common.KrimsonPlugin;
import ovh.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.compat.stream.output.OutputStreamHandler;

public class SpigotItemSerializer extends ItemSerializerHandler {
    public SpigotItemSerializer() {
        if(!messageSent) {
            KrimsonPlugin.getInstance().getLogger().info("Using Bukkit's ItemStack (de)serialization, which is less performant than Paper's one.");
            messageSent = true;
        }
    }

    @Override
    public void serializeAndWrite(ItemStack stack, OutputStreamHandler<?> outputStream) throws Exception {
        outputStream.writeObject(stack);
    }

    @Override
    public ItemStack readAndDeserialize(InputStreamHandler<?> inputStream, int length) throws Exception {
        return (ItemStack) inputStream.readObject();
    }
}
