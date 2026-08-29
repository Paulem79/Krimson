package net.paulem.krimson.mobs.animation;

import net.paulem.krimson.mobs.CustomMobType;
import net.paulem.krimson.models.blockbench.rig.ModelInstance;

/**
 * The GeckoLib-ish part of the system: a small per-instance state machine that decides,
 * every tick, which of the mob's animations should be playing on its {@link ModelInstance}
 * rig, and asks it to cross-fade there when the decision changes.
 *
 * <p>Movement/idle are inferred automatically from the entity's own velocity. Attack/hurt
 * are one-shot triggers with an author-configured duration (since Bukkit doesn't expose a
 * clean "animation finished" callback the way the client-only GeckoLib does); death is
 * permanent once triggered.
 */
public final class MobAnimationController {
    private final CustomMobType<?> type;
    private final ModelInstance rig;

    private MobAnimState state = MobAnimState.IDLE;
    private float stateTimeRemaining;
    private boolean dead;

    public MobAnimationController(CustomMobType<?> type, ModelInstance rig) {
        this.type = type;
        this.rig = rig;
        apply(MobAnimState.IDLE);
    }

    public MobAnimState state() {
        return state;
    }

    public void triggerHurt() {
        if (dead) {
            return;
        }
        enter(MobAnimState.HURT, type.hurtAnimSeconds());
    }

    public void triggerAttack() {
        if (dead) {
            return;
        }
        enter(MobAnimState.ATTACK, type.attackAnimSeconds());
    }

    /** Plays an animation not covered by the built-in states, by raw name in the bbmodel. */
    public void triggerCustom(String animationName, float seconds) {
        if (dead) {
            return;
        }
        state = MobAnimState.CUSTOM;
        stateTimeRemaining = Math.max(0.05F, seconds);
        rig.play(animationName, 0.1F);
    }

    public void triggerDeath() {
        if (dead) {
            return;
        }
        dead = true;
        apply(MobAnimState.DEATH);
    }

    /**
     * Advances the state machine.
     *
     * @param deltaSeconds real time since the last call
     * @param moving       whether the backing entity is currently under way (from position delta)
     */
    public void tick(float deltaSeconds, boolean moving) {
        if (dead) {
            return; // DEATH is terminal: never leave it once triggered.
        }
        if (state.isTransient()) {
            stateTimeRemaining -= deltaSeconds;
            if (stateTimeRemaining > 0.0F) {
                return; // still mid hurt/attack/custom: let it finish before re-evaluating locomotion
            }
        }

        MobAnimState desired = moving ? MobAnimState.WALK : MobAnimState.IDLE;
        if (desired != state) {
            apply(desired);
        }
    }

    private void enter(MobAnimState newState, float seconds) {
        state = newState;
        stateTimeRemaining = Math.max(0.05F, seconds);
        apply(newState);
    }

    private void apply(MobAnimState newState) {
        state = newState;
        String animationName = type.animationFor(newState.name().toLowerCase());
        if (animationName != null) {
            rig.play(animationName, newState == MobAnimState.DEATH ? 0.1F : 0.15F);
        }
    }
}
