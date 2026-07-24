package net.paulem.krimsontest.entities.custom;

import net.paulem.krimson.entities.custom.ModelEntityProperties;
import org.bukkit.entity.LivingEntity;

/**
 * Properties for the animated mob entity
 */
public class AnimatedMobEntityProperties extends ModelEntityProperties {
    public AnimatedMobEntityProperties(LivingEntity entity, AnimatedMobEntity animatedMobEntity) {
        super(entity, animatedMobEntity);
    }

    // Can add custom properties here
}