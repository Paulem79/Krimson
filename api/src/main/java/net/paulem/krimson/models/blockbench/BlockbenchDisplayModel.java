package net.paulem.krimson.models.blockbench;

import lombok.Getter;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.models.Model;
import net.paulem.krimson.models.blockbench.model.BbModel;
import net.paulem.krimson.models.blockbench.model.BbModelLoader;
import net.paulem.krimson.models.blockbench.rig.RigManifest;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.joml.Vector3f;

import java.io.InputStream;

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
            KrimsonPlugin.getInstance().getLogger().severe("Could not load the model or rig manifest for " + key);
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

    private boolean loadAssets() {
        try (InputStream modelStream = KrimsonPlugin.getInstance().getResource("assets/" + key.getNamespace() + "/" + key.getKey() + ".bbmodel");
             InputStream rigStream = KrimsonPlugin.getInstance().getResource("assets/" + key.getNamespace() + "/rig.json")) {
            if (modelStream == null || rigStream == null) {
                KrimsonPlugin.getInstance().getLogger().severe(key.getKey() + ".bbmodel or rig.json missing from the jar.");
                return false;
            }
            this.model = BbModelLoader.load(modelStream);
            this.manifest = RigManifest.load(rigStream);
            return true;
        } catch (Exception exception) {
            KrimsonPlugin.getInstance().getLogger().severe("Failed to load assets: " + exception);
            return false;
        }
    }
}
