package net.paulem.krimson.mobs.nms;

import net.minecraft.world.entity.Mob;

/**
 * Implemented by every Krimson NMS mob base class ({@link CustomZombieEntity},
 * {@link CustomHorseEntity}, {@link CustomIronGolemEntity}, or any class you write following
 * the same pattern for another vanilla entity).
 *
 * <p>Each of these classes intentionally leaves {@code registerGoals()} untouched (whatever
 * the vanilla superclass does there just runs and gets thrown away) and instead
 * {@link #clearVanillaGoals()} wipes it once the entity has been constructed. Krimson's own
 * AI ({@code net.paulem.krimson.mobs.ai}) does not touch {@code GoalSelector}/{@code Goal}
 * at all — it runs entirely against the public {@code org.bukkit.entity.Mob} API from
 * {@code CustomMobManager}'s tick loop, via {@link net.paulem.krimson.mobs.ai.KrimsonGoalSelector}.
 * This method exists purely so the mob doesn't keep acting on vanilla's own default AI
 * (attacking villagers as a zombie, etc.) underneath the Krimson one.
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
    /** Clears whatever goals the vanilla constructor registered. Called once, right after construction. */
    void clearVanillaGoals();

    /** Fired by {@code CustomMobListener} on {@code EntityDamageEvent}; drives the hurt animation. */
    default void onCustomHurt() {
    }

    /** Fired by {@code CustomMobListener} on {@code EntityDeathEvent}; drives the death animation. */
    default void onCustomDeath() {
    }
}
