package net.paulem.krimsontest.sounds;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.sounds.CustomSound;
import net.paulem.krimson.sounds.Sounds;

/**
 * Test plugin sounds — mirrors the pattern of
 * {@link net.paulem.krimsontest.blocks.PluginBlocks},
 * {@link net.paulem.krimsontest.items.PluginItems}, and
 * {@link net.paulem.krimsontest.models.PluginModels}.
 */
public class PluginSounds {
    private PluginSounds() {
        /* This utility class should not be instantiated */
    }

    public static final CustomSound TEST_SOUND = Sounds.registerSound("test_sound", CustomSound::new);

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering sounds...");
        Sounds.REGISTRY.freeze();
    }
}
