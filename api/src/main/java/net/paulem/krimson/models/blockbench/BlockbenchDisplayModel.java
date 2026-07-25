package net.paulem.krimson.models.blockbench;

import lombok.Getter;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.models.Model;
import net.paulem.krimson.models.blockbench.model.BbModel;
import net.paulem.krimson.models.blockbench.model.BbModelBaker;
import net.paulem.krimson.models.blockbench.model.BbModelLoader;
import net.paulem.krimson.models.blockbench.model.BlockbenchModelAssets;
import net.paulem.krimson.models.blockbench.rig.RigManifest;
import net.paulem.krimson.models.blockbench.rig.RigPart;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BlockbenchDisplayModel implements Model {
    @Getter
    private final NamespacedKey key;

    @Getter
    private BbModel model;
    @Getter
    private RigManifest manifest;
    @Getter
    private RigManager rigs;

    public BlockbenchDisplayModel(NamespacedKey key) {
        this.key = key;

        if (!loadAssets()) {
            KrimsonPlugin.getInstance().getLogger().severe("Could not load or bake the model for " + key);
            return;
        }

        Material carrier = Material.PAPER;

        Vector3f originOffset = new Vector3f(0, 0, 0);

        rigs = new RigManager(KrimsonPlugin.getInstance(), model, manifest, carrier,1, originOffset);
        rigs.start();

        KrimsonPlugin.getInstance().getLogger().info(String.format(
                "%s Ready: %d bones, %d animations, %d rig parts (%d visible by default).",
                key, model.bones.size(), model.animations.size(), manifest.size(),
                manifest.parts().stream().filter(p -> p.visibleByDefault).count()));
    }

    /**
     * Loads the raw {@code .bbmodel} and bakes it into item models, textures and a rig
     * manifest, all in memory. Nothing but the {@code .bbmodel} needs to ship in the jar
     * any more: the resource pack the client downloads (via {@code ResourcePack.kt}) and
     * the rig manifest the plugin animates from are both generated from it here, so there
     * is no separate offline generation step and no pack for anyone to install by hand.
     */
    private boolean loadAssets() {
        try (InputStream modelStream = KrimsonPlugin.getInstance().getResource(
                "assets/" + key.getNamespace() + "/" + key.getKey() + ".bbmodel")) {
            if (modelStream == null) {
                KrimsonPlugin.getInstance().getLogger().severe(key.getKey() + ".bbmodel missing from the jar.");
                return false;
            }
            this.model = BbModelLoader.load(modelStream);

            BbModelBaker.BakeResult baked = BbModelBaker.bake(model, key.getKey());
            for (String warning : baked.warnings()) {
                KrimsonPlugin.getInstance().getLogger().warning(key + ": " + warning);
            }

            BlockbenchModelAssets.ModelAssets assets = new BlockbenchModelAssets.ModelAssets();
            assets.textures.putAll(baked.textures());

            List<RigPart> rigParts = new ArrayList<>();
            for (BbModelBaker.BakedPart part : baked.parts()) {
                assets.itemModels.put(part.id(), part.itemModelJson());
                rigParts.add(new RigPart(part.id(), part.bone(),
                        new NamespacedKey(key.getNamespace(), part.id()),
                        part.center(), part.rotation(), part.pivot(), part.visibleByDefault()));
            }
            BlockbenchModelAssets.register(key.toString(), assets);

            this.manifest = RigManifest.of(rigParts);
            return true;
        } catch (Exception exception) {
            KrimsonPlugin.getInstance().getLogger().severe("Failed to load/bake assets: " + exception);
            return false;
        }
    }
}
