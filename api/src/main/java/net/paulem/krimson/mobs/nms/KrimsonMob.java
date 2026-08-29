package net.paulem.krimson.mobs.nms;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.paulem.krimson.mobs.CustomMobType;

/**
 * Implemented by every Krimson NMS mob base class ({@link CustomZombieEntity},
 * {@link CustomHorseEntity}, {@link CustomIronGolemEntity}, or any class you write following
 * the same pattern for another vanilla entity).
 *
 * <p>Each of these classes intentionally leaves {@code registerGoals()} untouched (whatever
 * the vanilla superclass does there just runs and gets thrown away). Vanilla calls
 * {@code registerGoals()} from deep inside the {@code Mob} constructor, i.e. <em>before</em>
 * this instance's own fields (like which {@code CustomMobType} it is) have been assigned -
 * so building the goal list there would only ever see nulls. Instead,
 * {@code CustomMobManager} calls {@link #applyGoals(CustomMobType)} itself immediately after
 * construction, once the definition is available. The goal/target selectors are real
 * vanilla {@link GoalSelector}s either way; this is purely about *when* they get populated,
 * not *what* populates them.
 *
 * <h2>Adding a base for another vanilla entity</h2>
 * Copy one of the three existing classes and change what it extends - it has to be the
 * exact concrete NMS class matching the {@code EntityType} you plan to spawn it as (e.g. to
 * build on {@code EntityType.RAVAGER}, extend {@code net.minecraft.world.entity.raid.Ravager}
 * directly, not {@code Monster}). This is a hard requirement, not a style choice: Paper's
 * {@code CraftEntity.getEntity(...)} resolves a spawned entity's Bukkit wrapper by looking
 * at its {@code EntityType} and then hard-casting to the matching NMS class, so a body whose
 * Java class only shares an ancestor with that type throws a {@code ClassCastException} on
 * every single spawn.
 */
public interface KrimsonMob<T extends Mob & KrimsonMob<T>> {
    void applyGoals(CustomMobType<T> type);

    /** Fired by {@code CustomMobListener} on {@code EntityDamageEvent}; drives the hurt animation. */
    default void onCustomHurt() {
    }

    /** Fired by {@code CustomMobListener} on {@code EntityDeathEvent}; drives the death animation. */
    default void onCustomDeath() {
    }
}
