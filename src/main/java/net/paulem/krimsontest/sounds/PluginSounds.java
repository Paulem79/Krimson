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
    public static final CustomSound SUMMON_STAND = Sounds.registerSound("summon_stand", CustomSound::new);
    public static final CustomSound SUMMON_KILLER_QUEEN = Sounds.registerSound("summon_killer_queen", CustomSound::new);
    public static final CustomSound STAND_THEWORLD_MUDA2 = Sounds.registerSound("stand_theworld_muda2", CustomSound::new);
    public static final CustomSound STAND_THEWORLD_MUDA3 = Sounds.registerSound("stand_theworld_muda3", CustomSound::new);
    public static final CustomSound THEWORLD_MUDA = Sounds.registerSound("theworld_muda", CustomSound::new);

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering sounds...");
        Sounds.REGISTRY.freeze();
    }
}
