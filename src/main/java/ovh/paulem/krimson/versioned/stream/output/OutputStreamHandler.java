package ovh.paulem.krimson.versioned.stream.output;

import ovh.paulem.krimson.versioned.Versioning;

import java.io.*;

public abstract class OutputStreamHandler<T extends OutputStream> {
    protected final T dataOutput;

    public OutputStreamHandler(ByteArrayOutputStream outputStream) {
        this.dataOutput = create(outputStream);
    }

    public abstract T create(ByteArrayOutputStream outputStream);

    public abstract void writeInt(int value) throws IOException;

    public abstract void writeUTF(String value) throws IOException;

    public abstract void write(byte[] bytes) throws IOException;

    public abstract void close() throws IOException;

    public abstract void writeObject(Object object) throws IOException;

    public static OutputStreamHandler<?> getHandler(ByteArrayOutputStream outputStream) {
        return Versioning.isPaper() ? new PaperOutputStream(outputStream) : new BukkitOutputStream(outputStream);
    }
}
