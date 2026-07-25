package net.paulem.krimson.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonLoader {
    private JsonLoader() {
        /* This utility class should not be instantiated */
    }

    @Nullable
    public static JsonObject loadNullableJson(String fileName) {
        try {
            return loadJson(fileName);
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonObject loadJson(String fileName) {
        try (InputStream stream = JsonLoader.class.getClassLoader().getResourceAsStream(fileName)) {

            if (stream == null) {
                throw new RuntimeException("Resource not found: " + fileName);
            }

            return JsonParser.parseReader(new InputStreamReader(stream))
                    .getAsJsonObject();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON", e);
        }
    }
}