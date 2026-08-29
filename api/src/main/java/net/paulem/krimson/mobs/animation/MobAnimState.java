package net.paulem.krimson.mobs.animation;

/**
 * The built-in animation states {@link MobAnimationController} arbitrates between every
 * tick. Each maps to a named animation inside the mob's {@code .bbmodel} through
 * {@code CustomMobType#animations()}; a state with no mapping simply holds the model at
 * rest (or, for transient states, is skipped).
 *
 * <p>Priority is the enum's declaration order: {@link #DEATH} always wins, then
 * {@link #HURT}, then {@link #ATTACK}, then movement, then {@link #IDLE}. This mirrors how
 * GeckoLib-style controllers usually layer "action" animations over locomotion.
 */
public enum MobAnimState {
    /** Plays once, then the rig is torn down; highest priority, never interrupted. */
    DEATH(true, true),
    /** Brief reaction to taking damage; interrupts movement/idle but not death. */
    HURT(true, false),
    /** An attack swing, triggered explicitly by a goal via {@code CustomMobInstance#triggerAttack()}. */
    ATTACK(true, false),
    /** Playing while the mob is actually moving under pathfinding control. */
    WALK(false, false),
    /** Standing still. The default/fallback state. */
    IDLE(false, false),
    /** Escape hatch for anything a custom goal wants to drive directly by name. */
    CUSTOM(true, false);

    private final boolean transient_;
    private final boolean terminal;

    MobAnimState(boolean transient_, boolean terminal) {
        this.transient_ = transient_;
        this.terminal = terminal;
    }

    /** True if this state should play once and then fall back rather than loop forever. */
    public boolean isTransient() {
        return transient_;
    }

    /** True if reaching this state ends the mob's animation lifecycle (death). */
    public boolean isTerminal() {
        return terminal;
    }
}
