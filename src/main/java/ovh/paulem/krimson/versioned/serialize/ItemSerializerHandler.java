package ovh.paulem.krimson.versioned.serialize;

import org.bukkit.inventory.ItemStack;
import ovh.paulem.krimson.versioned.Versioning;
import ovh.paulem.krimson.versioned.stream.input.InputStreamHandler;
import ovh.paulem.krimson.versioned.stream.output.OutputStreamHandler;

public abstract class ItemSerializerHandler {
    public abstract void serializeAndWrite(ItemStack stack, OutputStreamHandler<?> outputStream) throws Exception;

    public abstract ItemStack readAndDeserialize(InputStreamHandler<?> inputStream, int length) throws Exception;

    public static ItemSerializerHandler getHandler() {
        return Versioning.isPaper() ? new PaperItemSerializer() : new BukkitItemSerializer();
    }
}
