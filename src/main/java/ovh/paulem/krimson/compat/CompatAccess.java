package ovh.paulem.krimson.compat;

import ovh.paulem.krimson.common.compat.Versioning;
import ovh.paulem.krimson.spigot.compat.serialize.SpigotItemSerializer;
import ovh.paulem.krimson.common.compat.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.paper.compat.serialize.PaperItemSerializer;
import ovh.paulem.krimson.spigot.compat.stream.input.SpigotInputStream;
import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;
import ovh.paulem.krimson.paper.compat.stream.input.PaperInputStream;
import ovh.paulem.krimson.spigot.compat.stream.output.SpigotOutputStream;
import ovh.paulem.krimson.common.compat.stream.output.OutputStreamHandler;
import ovh.paulem.krimson.paper.compat.stream.output.PaperOutputStream;

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
