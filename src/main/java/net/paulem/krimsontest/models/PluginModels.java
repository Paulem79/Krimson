package net.paulem.krimsontest.models;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.models.BDEngineModel;
import net.paulem.krimson.models.Models;
import net.paulem.krimson.models.blockbench.BlockbenchDisplayModel;

public class PluginModels {
    private PluginModels() {}

    // Modèle Legacy (Sans animation)
    public static final BDEngineModel SPEAKER = Models.registerModel("speaker",
            identifier -> new BDEngineModel(identifier, "/summon block_display ~-0.5 ~-0.5 ~-0.5 {Passengers:[{id:\"minecraft:block_display\",block_state:{Name:\"minecraft:polished_blackstone\",Properties:{}},transformation:[0.5557f,0f,0f,0.1875f,0f,1.1075f,0f,0f,0f,0f,0.4922f,0.25f,0f,0f,0f,1f]},{id:\"minecraft:item_display\",item:{id:\"minecraft:music_disc_stal\",Count:1},item_display:\"none\",transformation:[0.5273f,0f,0f,0.4375f,0f,0.758f,0f,0.8125f,0f,0f,0.4922f,0.25f,0f,0f,0f,1f]},{id:\"minecraft:item_display\",item:{id:\"minecraft:music_disc_stal\",Count:1},item_display:\"none\",transformation:[0.5273f,0f,0f,0.4375f,0f,0.758f,0f,0.3125f,0f,0f,0.4922f,0.25f,0f,0f,0f,1f]}]}")
    );

    public static final BDEngineModel ANIMATED_MODEL = Models.registerModel("animated_model",
            BDEngineModel::new
    );

    public static final BDEngineModel STONE_GATE = Models.registerModel("stone_gate",
            BDEngineModel::new
    );

    public static final BDEngineModel READING = Models.registerModel("reading",
            BDEngineModel::new
    );

    public static final BlockbenchDisplayModel THE_WORLD = Models.registerModel("the_world",
            identifier -> new BlockbenchDisplayModel(identifier, bone -> {
                // Compensation de 180° si c'est le groupe head principal, bug à régler
                if ("body".equalsIgnoreCase(bone.name)) {
                    return 180.0F;
                }
                return 0.0F;
            })
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering models...");
        Models.REGISTRY.freeze();
    }
}