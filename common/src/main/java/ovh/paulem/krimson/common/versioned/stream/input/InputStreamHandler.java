package ovh.paulem.krimson.common.versioned.stream.input;

import java.io.*;

public abstract class InputStreamHandler<T extends InputStream> {
    protected final T dataOutput;

    public InputStreamHandler(ByteArrayInputStream inputStream) {
        this.dataOutput = create(inputStream);
    }

    public abstract T create(ByteArrayInputStream inputStream);

    public abstract Object readObject() throws IOException, ClassNotFoundException;

    public abstract int readInt() throws IOException;

    public abstract String readUTF() throws IOException;

    public abstract int read(byte[] bytes) throws IOException;

    public abstract void close() throws IOException;

}
