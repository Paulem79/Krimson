package ovh.paulem.krimson.spigot.compat.stream.input;

import org.bukkit.util.io.BukkitObjectInputStream;
import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SpigotInputStream extends InputStreamHandler<BukkitObjectInputStream> {
    public SpigotInputStream(ByteArrayInputStream inputStream) {
        super(inputStream);
    }

    @Override
    public BukkitObjectInputStream create() {
        try {
            return new BukkitObjectInputStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return dataInput.readObject();
    }

    @Override
    public int readInt() throws IOException {
        return dataInput.readInt();
    }

    @Override
    public String readUTF() throws IOException {
        return dataInput.readUTF();
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        return dataInput.read(bytes);
    }

    @Override
    public void close() throws IOException {
        dataInput.close();
    }
}
