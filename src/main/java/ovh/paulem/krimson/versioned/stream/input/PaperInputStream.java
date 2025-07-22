package ovh.paulem.krimson.versioned.stream.input;

import ovh.paulem.krimson.Krimson;

import java.io.*;

public class PaperInputStream extends InputStreamHandler<DataInputStream> {
    public PaperInputStream(ByteArrayInputStream inputStream) {
        super(inputStream);

        Krimson.getInstance().getLogger().info("Using Paper's input stream handler for serialization, we love performance!");
    }

    @Override
    public DataInputStream create(ByteArrayInputStream inputStream) {
        return new DataInputStream(inputStream);
    }

    @Override
    public Object readObject() {
        throw new RuntimeException("Paper's input stream handler doesn't support reading objects!");
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
    public void close() {

    }
}
