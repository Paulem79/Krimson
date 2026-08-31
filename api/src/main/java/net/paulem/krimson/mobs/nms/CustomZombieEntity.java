package net.paulem.krimson.mobs.nms;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * A custom mob whose body is a real {@code Zombie}. It has to extend {@code Zombie}
 * concretely, not just {@code Monster}: Paper's {@code CraftEntity.getEntity(...)} looks at
 * the entity's {@code EntityType} to pick a Bukkit wrapper (e.g. {@code CraftZombie}) and
 * then does a hard {@code (Zombie) entity} cast to build it. An instance whose Java class
 * only shares an ancestor with {@code Zombie} fails that cast with a
 * {@code ClassCastException} the moment anything asks for its Bukkit entity (which happens
 * on every spawn, since {@code CreatureSpawnEvent} needs one) — this is exactly the crash
 * from the previous version of this file. Extending the concrete class is what makes
 * {@code instanceof Zombie} (and therefore the cast) actually true.
 *
 * <p>Zombie's own {@code registerGoals()} still runs during {@code super()} and adds
 * vanilla zombie goals; {@link #applyGoals} wipes them and installs the ones from your
 * {@link CustomMobType} instead, once the definition is available (see {@link KrimsonMob}
 * for why that can't happen during construction itself). Everything else Zombie does for
 * free — burning in daylight, converting to a drowned in water, baby-zombie riding logic —
 * still works, since none of that is touched here.
 */
public class CustomZombieEntity extends Zombie implements KrimsonMob<CustomZombieEntity> {
    public CustomZombieEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void clearVanillaGoals() {
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
    }
}
