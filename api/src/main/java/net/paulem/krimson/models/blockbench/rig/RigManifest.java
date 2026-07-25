package net.paulem.krimson.models.blockbench.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.NamespacedKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The {@code rig.json} produced alongside the resource pack.
 *
 * <p>It is the contract between the two halves of this prototype: the generator decides
 * how cubes are grouped into parts and where each part's geometry was centred, and the
 * plugin reads that back so it can compute matrices without duplicating the grouping
 * logic (and without being able to drift out of sync with the pack).
 */
public final class RigManifest {
    private final List<RigPart> parts;

    private RigManifest(List<RigPart> parts) {
        this.parts = Collections.unmodifiableList(parts);
    }

    public List<RigPart> parts() {
        return parts;
    }

    public int size() {
        return parts.size();
    }

    /**
     * Builds a manifest directly from already-computed parts — how the manifest is
     * produced now that it's generated from the {@code .bbmodel} at runtime instead of
     * being read from a {@code rig.json} written by an offline tool.
     */
    public static RigManifest of(List<RigPart> parts) throws IOException {
        if (parts.isEmpty()) {
            throw new IOException("no rig parts were baked from the model");
        }
        return new RigManifest(new ArrayList<>(parts));
    }

    public static RigManifest load(InputStream input) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String namespace = root.has("namespace")
                    ? root.get("namespace").getAsString()
                    : "bbproto";

            List<RigPart> parts = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("parts")) {
                JsonObject json = element.getAsJsonObject();
                String id = json.get("id").getAsString();
                String key = json.has("itemModel")
                        ? json.get("itemModel").getAsString()
                        : namespace + ":" + id;
                parts.add(new RigPart(
                        id,
                        json.get("bone").getAsString(),
                        parseKey(key),
                        readVec3(json, "center"),
                        readVec3(json, "rotation"),
                        readVec3(json, "pivot"),
                        !json.has("visible") || json.get("visible").getAsBoolean()));
            }
            if (parts.isEmpty()) {
                throw new IOException("rig.json contains no parts");
            }
            return new RigManifest(parts);
        }
    }

    @SuppressWarnings("deprecation")
    private static NamespacedKey parseKey(String raw) {
        int colon = raw.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : raw.substring(0, colon);
        String value = colon < 0 ? raw : raw.substring(colon + 1);
        return new NamespacedKey(namespace, value);
    }

    private static float[] readVec3(JsonObject json, String key) {
        float[] out = new float[3];
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return out;
        }
        JsonArray array = json.getAsJsonArray(key);
        for (int i = 0; i < 3 && i < array.size(); i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }
}
