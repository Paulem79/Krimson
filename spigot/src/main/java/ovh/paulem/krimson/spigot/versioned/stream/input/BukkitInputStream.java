package ovh.paulem.krimson.spigot.versioned.stream.input;

import org.bukkit.util.io.BukkitObjectInputStream;
import ovh.paulem.krimson.common.versioned.stream.input.InputStreamHandler;

import java.io.*;

public class BukkitInputStream extends InputStreamHandler<BukkitObjectInputStream> {
    public BukkitInputStream(ByteArrayInputStream inputStream) {
        super(inputStream);
    }

    @Override
    public BukkitObjectInputStream create(ByteArrayInputStream inputStream) {
        try {
            return new BukkitObjectInputStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return dataOutput.readObject();
    }

    @Override
    public int readInt() throws IOException {
        return dataOutput.readInt();
    }

    @Override
    public String readUTF() throws IOException {
        return dataOutput.readUTF();
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        return dataOutput.read(bytes);
    }

    @Override
    public void close() throws IOException {
        dataOutput.close();
    }
}
