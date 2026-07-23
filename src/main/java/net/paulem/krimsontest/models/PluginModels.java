package net.paulem.krimsontest.models;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.models.BlockDisplayModel;
import net.paulem.krimson.models.Models;
import net.paulem.krimson.utils.JsonLoader;

public class PluginModels {
    private PluginModels() {}

    // Modèle Legacy (Sans animation)
    public static final BlockDisplayModel SPEAKER = Models.registerModel("speaker",
            identifier -> new BlockDisplayModel(identifier, "/summon block_display ~-0.5 ~-0.5 ~-0.5 {Passengers:[{id:\"minecraft:block_display\",block_state:{Name:\"minecraft:polished_blackstone\",Properties:{}},transformation:[0.5557f,0f,0f,0.1875f,0f,1.1075f,0f,0f,0f,0f,0.4922f,0.25f,0f,0f,0f,1f]},{id:\"minecraft:item_display\",item:{id:\"minecraft:music_disc_stal\",Count:1},item_display:\"none\",transformation:[0.5273f,0f,0f,0.4375f,0f,0.758f,0f,0.8125f,0f,0f,0.4922f,0.25f,0f,0f,0f,1f]},{id:\"minecraft:item_display\",item:{id:\"minecraft:music_disc_stal\",Count:1},item_display:\"none\",transformation:[0.5273f,0f,0f,0.4375f,0f,0.758f,0f,0.3125f,0f,0f,0.4922f,0.25f,0f,0f,0f,1f]}]}")
    );

    // Modèle JSON (Avec animations depuis server-api.json)
    public static final BlockDisplayModel ANIMATED_MODEL = Models.registerModel("animated_model",
            identifier -> new BlockDisplayModel(identifier, JsonLoader.loadJson("assets/server-api.json"))
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering items & models...");
        Models.REGISTRY.freeze();
    }
}