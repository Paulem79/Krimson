package ovh.paulem.krimson.spigot.versioned.serialize;

import org.bukkit.inventory.ItemStack;
import ovh.paulem.krimson.common.KrimsonPlugin;
import ovh.paulem.krimson.common.versioned.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.common.versioned.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.versioned.stream.output.OutputStreamHandler;

public class BukkitItemSerializer extends ItemSerializerHandler {
    public BukkitItemSerializer() {
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
