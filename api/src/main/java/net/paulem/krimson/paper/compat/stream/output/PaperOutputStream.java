package net.paulem.krimson.paper.compat.stream.output;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PaperOutputStream {
    protected final ByteArrayOutputStream outputStream;
    protected final DataOutputStream dataOutput;

    public PaperOutputStream(ByteArrayOutputStream outputStream) {
        this.outputStream = outputStream;
        this.dataOutput = create();
    }

    public DataOutputStream create() {
        return new DataOutputStream(outputStream);
    }

    public void writeInt(int value) throws IOException {
        dataOutput.writeInt(value);
    }

    public void writeUTF(String value) throws IOException {
        dataOutput.writeUTF(value);
    }

    public void write(byte[] bytes) throws IOException {
        dataOutput.write(bytes);
    }

    public byte[] toByteArray() {
        return outputStream.toByteArray();
    }
}
