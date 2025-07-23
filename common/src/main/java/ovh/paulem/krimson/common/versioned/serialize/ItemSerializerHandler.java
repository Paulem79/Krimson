package ovh.paulem.krimson.common.versioned.serialize;

import org.bukkit.inventory.ItemStack;
import ovh.paulem.krimson.common.versioned.stream.input.InputStreamHandler;
import ovh.paulem.krimson.common.versioned.stream.output.OutputStreamHandler;

public abstract class ItemSerializerHandler {
    protected static boolean messageSent = false;

    public abstract void serializeAndWrite(ItemStack stack, OutputStreamHandler<?> outputStream) throws Exception;

    public abstract ItemStack readAndDeserialize(InputStreamHandler<?> inputStream, int length) throws Exception;

}
