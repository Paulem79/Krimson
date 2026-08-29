package net.paulem.krimson.mobs.nms;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.paulem.krimson.mobs.CustomMobType;

/**
 * A custom mob whose body is a real {@code Horse} — a good base for anything that wants a
 * tall passive-mob hitbox (a giraffe, say). See {@link CustomZombieEntity} for why this has
 * to extend the concrete vanilla class rather than a shared ancestor like {@code Animal}.
 *
 * <p>Horse brings taming/saddle/inventory behaviour along for free, which you may not want
 * for a plain wildlife mob. If not, the simplest fix is to keep it un-tameable in practice
 * by never handing out a saddle/name-tag interaction path in your own item design, or to
 * override the relevant {@code mobInteract}/{@code isTamed} methods here if you need the
 * horse-specific UI fully suppressed.
 */
public class CustomHorseEntity extends Horse implements KrimsonMob<CustomHorseEntity> {
    public CustomHorseEntity(EntityType<? extends Horse> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void applyGoals(CustomMobType<CustomHorseEntity> type) {
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        type.goalConfigurator().configure(this, this.goalSelector, this.targetSelector);
    }
}
