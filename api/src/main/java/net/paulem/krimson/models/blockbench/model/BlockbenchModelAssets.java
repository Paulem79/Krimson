package net.paulem.krimson.models.blockbench.model;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global registry (keyed by model key) of the assets baked out of a {@code .bbmodel}:
 * raw PNG textures + one item model json per rig part. {@code ResourcePack.kt} reads this
 * registry when building the pack (see {@code addBBModelAssets}), exactly like it already
 * reads {@code Items.REGISTRY} / {@code Sounds.REGISTRY} / {@code UIRegistry.REGISTRY}.
 *
 * <p>Populated by {@link BbModelBaker} via {@link BlockbenchDisplayModel} at load time, so
 * the pack always reflects whatever {@code .bbmodel} is shipped — no separate offline
 * generation step needed.
 */
public final class BlockbenchModelAssets {
    private BlockbenchModelAssets() {
    }

    public static final Map<String, ModelAssets> REGISTRY = new LinkedHashMap<>();

    public static final class ModelAssets {
        /** texture name -> raw PNG bytes. */
        public final Map<String, byte[]> textures = new LinkedHashMap<>();
        /** item model key suffix -> item model json. */
        public final Map<String, JsonObject> itemModels = new LinkedHashMap<>();
    }

    public static void register(String modelKey, ModelAssets assets) {
        REGISTRY.put(modelKey, assets);
    }
}
