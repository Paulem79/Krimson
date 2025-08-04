package ovh.paulem.krimson.common.compat.stream.output;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class OutputStreamHandler<T extends OutputStream> {
    protected final ByteArrayOutputStream outputStream;
    protected final T dataOutput;

    public OutputStreamHandler(ByteArrayOutputStream outputStream) {
        this.outputStream = outputStream;
        this.dataOutput = create();
    }

    public abstract T create();

    public abstract void writeInt(int value) throws IOException;

    public abstract void writeUTF(String value) throws IOException;

    public abstract void write(byte[] bytes) throws IOException;

    public abstract void close() throws IOException;

    public abstract void writeObject(Object object) throws IOException;

    public byte[] toByteArray() {
        return outputStream.toByteArray();
    }

}
