package ovh.paulem.krimson.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class ZLibUtils {
    /**
     * Compresses the input byte array using the ZLIB compression algorithm.
     *
     * @param data the byte array to be compressed
     * @return a compressed byte array
     * @throws IOException if an I/O error occurs during compression
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (DeflaterOutputStream dos = new DeflaterOutputStream(bos)) {
            dos.write(data);
        }

        return bos.toByteArray();
    }

    /**
     * Decompresses a given byte array that was compressed using the ZLIB compression algorithm.
     *
     * @param data the compressed byte array to be decompressed
     * @return a decompressed byte array
     * @throws IOException if an I/O error occurs during decompression
     */
    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(data))) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = iis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
        }

        return bos.toByteArray();
    }
}
