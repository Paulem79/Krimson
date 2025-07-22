package ovh.paulem.krimson.versioned;

import ovh.paulem.krimson.common.versioned.Versioning;
import ovh.paulem.krimson.spigot.versioned.serialize.BukkitItemSerializer;
import ovh.paulem.krimson.common.versioned.serialize.ItemSerializerHandler;
import ovh.paulem.krimson.paper.versioned.serialize.PaperItemSerializer;
import ovh.paulem.krimson.spigot.versioned.stream.input.BukkitInputStream;
import ovh.paulem.krimson.common.versioned.stream.input.InputStreamHandler;
import ovh.paulem.krimson.paper.versioned.stream.input.PaperInputStream;
import ovh.paulem.krimson.spigot.versioned.stream.output.BukkitOutputStream;
import ovh.paulem.krimson.common.versioned.stream.output.OutputStreamHandler;
import ovh.paulem.krimson.paper.versioned.stream.output.PaperOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Access {
    public static ItemSerializerHandler getHandler() {
        return Versioning.isPaper() ? new PaperItemSerializer() : new BukkitItemSerializer();
    }

    public static OutputStreamHandler<?> getHandler(ByteArrayOutputStream outputStream) {
        return Versioning.isPaper() ? new PaperOutputStream(outputStream) : new BukkitOutputStream(outputStream);
    }

    public static InputStreamHandler<?> getHandler(ByteArrayInputStream inputStream) {
        return Versioning.isPaper() ? new PaperInputStream(inputStream) : new BukkitInputStream(inputStream);
    }
}
