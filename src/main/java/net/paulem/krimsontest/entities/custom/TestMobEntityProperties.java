package net.paulem.krimsontest.entities.custom;

import net.paulem.krimson.entities.custom.CustomEntityProperties;
import org.bukkit.entity.LivingEntity;

/**
 * Properties for the test mob entity
 */
public class TestMobEntityProperties extends CustomEntityProperties {
    public TestMobEntityProperties(LivingEntity entity, TestMobEntity testMobEntity) {
        super(entity, testMobEntity);
    }

    // Can add custom properties here
}