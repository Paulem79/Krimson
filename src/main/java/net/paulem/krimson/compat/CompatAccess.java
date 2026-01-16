package net.paulem.krimson.compat;

import net.paulem.krimson.common.compat.Versioning;
import net.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import net.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import net.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import net.paulem.krimson.paper.compat.serialize.PaperItemSerializer;
import net.paulem.krimson.paper.compat.stream.input.PaperInputStream;
import net.paulem.krimson.paper.compat.stream.output.PaperOutputStream;
import net.paulem.krimson.spigot.compat.serialize.SpigotItemSerializer;
import net.paulem.krimson.spigot.compat.stream.input.SpigotInputStream;
import net.paulem.krimson.spigot.compat.stream.output.SpigotOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class CompatAccess {
    public static ItemSerializerHandler getHandler() {
        return Versioning.isPaper() ? new PaperItemSerializer() : new SpigotItemSerializer();
    }

    public static OutputStreamHandler<?> getHandler(ByteArrayOutputStream outputStream) {
        return Versioning.isPaper() ? new PaperOutputStream(outputStream) : new SpigotOutputStream(outputStream);
    }

    public static InputStreamHandler<?> getHandler(ByteArrayInputStream inputStream) {
        return Versioning.isPaper() ? new PaperInputStream(inputStream) : new SpigotInputStream(inputStream);
    }
}
