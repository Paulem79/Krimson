package net.paulem.krimson.compat;

import net.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import net.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import net.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import net.paulem.krimson.paper.compat.serialize.PaperItemSerializer;
import net.paulem.krimson.paper.compat.stream.input.PaperInputStream;
import net.paulem.krimson.paper.compat.stream.output.PaperOutputStream;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class CompatAccess {
    public static ItemSerializerHandler getHandler(JavaPlugin plugin) {
        return new PaperItemSerializer(plugin);
    }

    public static OutputStreamHandler<?> getHandler(ByteArrayOutputStream outputStream) {
        return new PaperOutputStream(outputStream);
    }

    public static InputStreamHandler<?> getHandler(ByteArrayInputStream inputStream) {
        return new PaperInputStream(inputStream);
    }
}
