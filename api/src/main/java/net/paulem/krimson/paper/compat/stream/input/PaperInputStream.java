package net.paulem.krimson.paper.compat.stream.input;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class PaperInputStream {
    protected final ByteArrayInputStream inputStream;
    protected final DataInputStream dataInput;

    public PaperInputStream(ByteArrayInputStream inputStream) {
        this.inputStream = inputStream;
        this.dataInput = create();
    }

    public DataInputStream create() {
        return new DataInputStream(inputStream);
    }

    public int readInt() throws IOException {
        return dataInput.readInt();
    }

    public String readUTF() throws IOException {
        return dataInput.readUTF();
    }

    public int read(byte[] bytes) throws IOException {
        return dataInput.read(bytes);
    }
}
