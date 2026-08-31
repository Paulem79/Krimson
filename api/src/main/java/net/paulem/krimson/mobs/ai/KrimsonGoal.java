package net.paulem.krimson.mobs.ai;

import org.bukkit.entity.Mob;

/**
 * One piece of AI behaviour for a {@code CustomMob}, written entirely against the public
 * Bukkit API — no {@code net.minecraft.world.entity.ai.goal.Goal}, no {@code GoalSelector}.
 *
 * <p>Movement/pathfinding still goes through {@link org.bukkit.entity.Mob#getPathfinder()}
 * under the hood, which is backed by NMS navigation inside Paper itself — but nothing in
 * this package (or in a plugin using it) touches an NMS class directly. That's the
 * boundary: Krimson's own code, and any plugin writing a goal, only ever sees
 * {@code org.bukkit.entity.*}.
 *
 * <p>Priority works like vanilla: within a {@link #category()}, the lowest priority number
 * whose {@link #canUse} is true wins and runs until {@link #canContinueToUse} goes false.
 */
public interface KrimsonGoal {

    /** Lower runs first within the same category. */
    int priority();

    GoalCategory category();

    /** Whether this goal wants to start running right now. */
    boolean canUse(Mob mob);

    /** Whether this goal, once running, should keep running. Defaults to re-checking {@link #canUse}. */
    default boolean canContinueToUse(Mob mob) {
        return canUse(mob);
    }

    default void start(Mob mob) {
    }

    default void tick(Mob mob, float deltaSeconds) {
    }

    default void stop(Mob mob) {
    }
}
