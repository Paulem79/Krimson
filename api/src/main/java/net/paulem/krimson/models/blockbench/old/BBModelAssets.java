package net.paulem.krimson.models.blockbench.old;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registre global (par clé de modèle) des assets générés à partir d'un
 * .bbmodel : textures PNG brutes + model json par bone. ResourcePack.kt lit
 * ce registre au moment de construire le pack (cf. addBBModelAssets),
 * exactement comme il lit déjà Items.REGISTRY / Sounds.REGISTRY / UIRegistry.REGISTRY.
 */
public final class BBModelAssets {
    private BBModelAssets() {}

    public static final Map<String, ModelAssets> REGISTRY = new LinkedHashMap<>();

    public static class ModelAssets {
        /** nom_texture -> bytes PNG (une seule texture dans la majorité des modèles bbmodel simples) */
        public final Map<String, byte[]> textures = new LinkedHashMap<>();
        /** suffixe de clé de modèle (part.modelKeySuffix) -> item model json */
        public final Map<String, JsonObject> itemModels = new LinkedHashMap<>();
    }

    public static void register(String modelKey, ModelAssets assets) {
        REGISTRY.put(modelKey, assets);
    }
}