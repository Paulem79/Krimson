package ovh.paulem.krimson.spigot.versioned.stream.output;

import org.bukkit.util.io.BukkitObjectOutputStream;
import ovh.paulem.krimson.common.versioned.stream.output.OutputStreamHandler;

import java.io.*;

public class BukkitOutputStream extends OutputStreamHandler<BukkitObjectOutputStream> {
    public BukkitOutputStream(ByteArrayOutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public BukkitObjectOutputStream create(ByteArrayOutputStream outputStream) {
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
