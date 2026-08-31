package net.paulem.krimsontest.models;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.models.bdengine.BDEngineModel;
import net.paulem.krimson.models.Models;
import net.paulem.krimson.models.blockbench.BlockbenchDisplayModel;

public class PluginModels {
    private PluginModels() {}

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
                // Compensation de 180° si c'est le groupe body principal
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