package ovh.paulem.krimson.versioned.stream.output;

import ovh.paulem.krimson.Krimson;

import java.io.*;

public class PaperOutputStream extends OutputStreamHandler<DataOutputStream> {
    public PaperOutputStream(ByteArrayOutputStream outputStream) {
        super(outputStream);

        Krimson.getInstance().getLogger().info("Using Paper's output stream handler for serialization, we love performance!");
    }

    @Override
    public DataOutputStream create(ByteArrayOutputStream outputStream) {
        return new DataOutputStream(outputStream);
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
    public void close() {

    }

    @Override
    public void writeObject(Object object) {
        throw new RuntimeException("Paper's output stream handler doesn't support writing objects!");
    }
}
