package net.paulem.krimson.common.compat.stream.input;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class InputStreamHandler<T extends InputStream> {
    protected final ByteArrayInputStream inputStream;
    protected final T dataInput;

    public InputStreamHandler(ByteArrayInputStream inputStream) {
        this.inputStream = inputStream;
        this.dataInput = create();
    }

    public abstract T create();

    public abstract Object readObject() throws IOException, ClassNotFoundException;

    public abstract int readInt() throws IOException;

    public abstract String readUTF() throws IOException;

    public abstract int read(byte[] bytes) throws IOException;

    public abstract void close() throws IOException;

}
