package net.paulem.krimson.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonLoader {
    private JsonLoader() {
        /* This utility class should not be instantiated */
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