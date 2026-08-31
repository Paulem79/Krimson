package net.paulem.krimson.mobs.ai;

/**
 * Which "slot" a {@link KrimsonGoal} competes for. Only one goal per category runs at a
 * time, chosen by priority — the same idea as vanilla's flag-based {@code GoalSelector},
 * just simplified to fixed categories since Krimson mobs don't need arbitrary flag
 * combinations.
 */
public enum GoalCategory {
    /** Controls the mob's target: who it's trying to fight. */
    TARGET,
    /** Controls movement: wandering, chasing, fleeing. */
    MOVEMENT,
    /** Controls head/body look direction. */
    LOOK,
    /** Anything else that doesn't need exclusivity (particles, sounds, custom triggers). */
    MISC
}
