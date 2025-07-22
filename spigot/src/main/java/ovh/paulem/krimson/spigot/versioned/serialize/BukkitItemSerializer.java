package ovh.paulem.krimson.spigot.versioned.serialize;

import org.bukkit.inventory.ItemStack;
import ovh.paulem.krimson.common.versioned.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.common.versioned.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.versioned.stream.output.OutputStreamHandler;

public class BukkitItemSerializer extends ItemSerializerHandler {
    @Override
    public void serializeAndWrite(ItemStack stack, OutputStreamHandler<?> outputStream) throws Exception {
        outputStream.writeObject(stack);
    }

    @Override
    public ItemStack readAndDeserialize(InputStreamHandler<?> inputStream, int length) throws Exception {
        return (ItemStack) inputStream.readObject();
    }
}
