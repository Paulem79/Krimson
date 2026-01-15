package ovh.paulem.krimson.utils;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for loading and using native (Rust) library functions via JNI.
 * Provides optimized implementations of performance-critical operations.
 */
public class NativeUtil {
    private static final Logger LOGGER = Logger.getLogger(NativeUtil.class.getName());
    private static final String LIBRARY_NAME = "krimson_native";
    private static boolean loaded = false;
    private static boolean attemptedLoad = false;

    static {
        tryLoadNativeLibrary();
    }

    /**
     * Attempts to load the native library.
     * First tries to load from java.library.path, then attempts to extract and load from resources.
     */
    private static void tryLoadNativeLibrary() {
        if (attemptedLoad) {
            return;
        }
        attemptedLoad = true;

        try {
            // Try loading from system library path first
            System.loadLibrary(LIBRARY_NAME);
            loaded = true;
            LOGGER.info("Successfully loaded native library: " + LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            LOGGER.log(Level.WARNING, "Failed to load native library from system path: " + e.getMessage());
            
            // Try to extract and load from resources
            try {
                loadLibraryFromResources();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load native library from resources: " + ex.getMessage());
                LOGGER.info("Native library not available. Will use fallback implementations.");
            }
        }
    }

    /**
     * Attempts to extract and load the native library from plugin resources.
     */
    private static void loadLibraryFromResources() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        String libFileName;
        String resourcePath;

        if (osName.contains("win")) {
            libFileName = LIBRARY_NAME + ".dll";
        } else if (osName.contains("mac")) {
            libFileName = "lib" + LIBRARY_NAME + ".dylib";
        } else {
            // Assume Linux/Unix
            libFileName = "lib" + LIBRARY_NAME + ".so";
        }

        resourcePath = "/native/" + libFileName;

        // Try to load from resources
        try (InputStream in = NativeUtil.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Native library not found in resources: " + resourcePath);
            }

            // Create temp file
            File tempLib = File.createTempFile(LIBRARY_NAME, libFileName.substring(libFileName.lastIndexOf('.')));
            tempLib.deleteOnExit();

            // Copy library to temp file
            Files.copy(in, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Load the library
            System.load(tempLib.getAbsolutePath());
            loaded = true;
            LOGGER.info("Successfully loaded native library from resources: " + resourcePath);
        }
    }

    /**
     * Checks if the native library was successfully loaded.
     *
     * @return true if the native library is available, false otherwise
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Parses a block key string in the format "x{num}y{num}z{num}" into [x, y, z] coordinates.
     * This is a native method implemented in Rust for optimal performance.
     * <p>
     * Expected format: x(\d+)y(-?\d+)z(\d+)
     * Example: "x5y64z10" -> [5, 64, 10]
     * Example: "x0y-64z15" -> [0, -64, 15]
     *
     * @param key the block key string to parse
     * @return an array of three integers [x, y, z], or null if parsing fails
     */
    @Nullable
    public static native int[] parseBlockKey(String key);
}
