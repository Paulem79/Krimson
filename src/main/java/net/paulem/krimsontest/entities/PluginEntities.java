package net.paulem.krimsontest.entities;

import net.paulem.krimson.entities.Entities;
import net.paulem.krimsontest.entities.custom.AnimatedMobEntity;
import net.paulem.krimsontest.entities.custom.TestMobEntity;
import org.bukkit.NamespacedKey;

public class PluginEntities {
    public static final TestMobEntity TEST_MOB = Entities.register(
        "test_mob",
        TestMobEntity::new
    );

    public static final AnimatedMobEntity ANIMATED_MOB = Entities.register(
        "animated_mob",
        AnimatedMobEntity::new
    );

    public static void init() {
        Entities.REGISTRY.freeze();
    }
}