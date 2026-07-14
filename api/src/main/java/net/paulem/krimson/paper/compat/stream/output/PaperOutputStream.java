package net.paulem.krimson.paper.compat.stream.output;

import net.paulem.krimson.common.compat.stream.output.OutputStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PaperOutputStream extends OutputStreamHandler<DataOutputStream> {
    public PaperOutputStream(ByteArrayOutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public DataOutputStream create() {
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
