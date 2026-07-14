package net.paulem.krimson.paper.compat.serialize;

import org.bukkit.inventory.ItemStack;
import net.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import net.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import net.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperItemSerializer extends ItemSerializerHandler {
    public PaperItemSerializer(JavaPlugin plugin) {
        if (!messageSent) {
            plugin.getLogger().info("Hooray! Using Paper's ItemStack (de)serialization, which gives you much better performance than Bukkit's!");
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
    public void serializeAndWrite(ItemStack[] stacks, OutputStreamHandler<?> outputStream) throws Exception {
        byte[] serialized = ItemStack.serializeItemsAsBytes(stacks);

        outputStream.writeInt(serialized.length);
        outputStream.write(serialized);
    }

    @Override
    public ItemStack readAndDeserialize(InputStreamHandler<?> inputStream, int length) throws Exception {
        byte[] itemBytes = new byte[length];
        inputStream.read(itemBytes);

        return ItemStack.deserializeBytes(itemBytes);
    }

    @Override
    public ItemStack[] readAndDeserializeList(InputStreamHandler<?> inputStream, int length) throws Exception {
        byte[] itemBytes = new byte[length];
        inputStream.read(itemBytes);

        return ItemStack.deserializeItemsFromBytes(itemBytes);
    }
}
