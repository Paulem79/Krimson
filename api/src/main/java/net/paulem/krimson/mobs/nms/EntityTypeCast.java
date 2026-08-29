package net.paulem.krimson.mobs.nms;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/**
 * Vanilla's {@code EntityType<T>} is generic-invariant: {@code EntityType.ZOMBIE} is typed
 * as {@code EntityType<Zombie>}, so it can't be passed directly where an
 * {@code EntityType<CustomZombieEntity>} is expected even though {@code CustomZombieEntity
 * extends Zombie}. This purely erases that mismatch for the compiler.
 *
 * <p><b>This only papers over generics - it changes nothing at runtime, and it is not a way
 * to reuse an {@code EntityType} with an unrelated Java class.</b> The object handed back
 * from your {@code MobFactory} still has to actually be an instance of the concrete NMS
 * class the {@code EntityType} expects, because Paper's {@code CraftEntity.getEntity(...)}
 * looks up that expected class from the {@code EntityType} and hard-casts to it when
 * building the Bukkit wrapper. Use this cast only alongside a base class that truly extends
 * the matching vanilla entity (see {@link KrimsonMob}'s class doc) - never with something
 * that only shares an ancestor.
 */
public final class EntityTypeCast {
    private EntityTypeCast() {
        /* utility class */
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> EntityType<T> as(EntityType<? extends Mob> vanillaType) {
        return (EntityType<T>) vanillaType;
    }
}
