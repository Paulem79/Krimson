package net.paulem.krimson.mobs.nms;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import net.paulem.krimson.mobs.CustomMobType;

/**
 * A custom mob whose body is a real {@code IronGolem} — a solid base for a boss: big
 * hitbox, slow, high knockback resistance by default. See {@link CustomZombieEntity} for
 * why this has to extend the concrete vanilla class rather than a shared ancestor like
 * {@code PathfinderMob}.
 *
 * <p>If your Paper/paperweight mappings put {@code IronGolem} under a different package
 * than {@code net.minecraft.world.entity.animal} (it has moved between versions), just fix
 * this one import — nothing else in the mob system depends on where this class lives.
 */
public class CustomIronGolemEntity extends IronGolem implements KrimsonMob<CustomIronGolemEntity> {
    public CustomIronGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void applyGoals(CustomMobType<CustomIronGolemEntity> type) {
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        type.goalConfigurator().configure(this, this.goalSelector, this.targetSelector);
    }
}
