package ovh.paulem.krimson.paper.compat.stream.input;

import ovh.paulem.krimson.common.compat.stream.input.InputStreamHandler;

import java.io.*;

public class PaperInputStream extends InputStreamHandler<DataInputStream> {
    public PaperInputStream(ByteArrayInputStream inputStream) {
        super(inputStream);
    }

    @Override
    public DataInputStream create() {
        return new DataInputStream(inputStream);
    }

    @Override
    public Object readObject() {
        throw new RuntimeException("Paper's input stream handler doesn't support reading objects!");
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
    public void close() {

    }
}
