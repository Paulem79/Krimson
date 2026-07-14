package net.paulem.krimson.paper.compat.serialize;

import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimson.paper.compat.stream.input.PaperInputStream;
import net.paulem.krimson.paper.compat.stream.output.PaperOutputStream;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperItemSerializer {
    public static final PaperItemSerializer INSTANCE = new PaperItemSerializer(KrimsonPlugin.getInstance());

    private PaperItemSerializer(JavaPlugin plugin) {
        plugin.getLogger().info("Hooray! Using Paper's ItemStack (de)serialization, which gives you much better performance than Bukkit's!");
    }

    public void serializeAndWrite(ItemStack stack, PaperOutputStream outputStream) throws Exception {
        byte[] serialized = stack.serializeAsBytes();

        outputStream.writeInt(serialized.length);
        outputStream.write(serialized);
    }

    public void serializeAndWrite(ItemStack[] stacks, PaperOutputStream outputStream) throws Exception {
        byte[] serialized = ItemStack.serializeItemsAsBytes(stacks);

        outputStream.writeInt(serialized.length);
        outputStream.write(serialized);
    }

    public ItemStack readAndDeserialize(PaperInputStream inputStream, int length) throws Exception {
        byte[] itemBytes = new byte[length];
        inputStream.read(itemBytes);

        return ItemStack.deserializeBytes(itemBytes);
    }

    public ItemStack[] readAndDeserializeList(PaperInputStream inputStream, int length) throws Exception {
        byte[] itemBytes = new byte[length];
        inputStream.read(itemBytes);

        return ItemStack.deserializeItemsFromBytes(itemBytes);
    }
}
