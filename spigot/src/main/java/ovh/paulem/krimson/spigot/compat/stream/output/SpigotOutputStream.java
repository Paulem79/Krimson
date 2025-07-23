package ovh.paulem.krimson.spigot.compat.stream.output;

import org.bukkit.util.io.BukkitObjectOutputStream;
import ovh.paulem.krimson.common.compat.stream.output.OutputStreamHandler;

import java.io.*;

public class SpigotOutputStream extends OutputStreamHandler<BukkitObjectOutputStream> {
    public SpigotOutputStream(ByteArrayOutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public BukkitObjectOutputStream create() {
        try {
            return new BukkitObjectOutputStream(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeInt(int value) throws IOException {
        dataOutput.writeInt(value);
    }

    @Override
    public void writeUTF(String value) throws IOException {
        dataOutput.writeUTF(value);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        dataOutput.write(bytes);
    }

    @Override
    public void close() throws IOException {
        dataOutput.close();
    }

    @Override
    public void writeObject(Object object) throws IOException {
        dataOutput.writeObject(object);
    }
}
