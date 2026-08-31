package net.paulem.krimson.mobs;

import net.paulem.krimson.mobs.ai.KrimsonGoalSelector;
import net.paulem.krimson.mobs.animation.MobAnimationController;
import net.paulem.krimson.mobs.boss.BossController;
import net.paulem.krimson.models.blockbench.rig.ModelInstance;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * One living, spawned custom mob: the real Bukkit entity, the {@link CustomMobType} it was
 * spawned from, the Blockbench rig puppeting its body, and (if it's a boss) its
 * {@link BossController}. Look one of these up for any tracked entity via
 * {@link CustomMobs#instanceOf(org.bukkit.entity.Entity)} - this is how a custom
 * {@code Goal} you wrote can trigger the attack animation, for instance.
 */
public final class CustomMobInstance {
    private final CustomMobType<?> type;
    private final Mob entity;
    private final ModelInstance rig;
    private final MobAnimationController animation;
    private final KrimsonGoalSelector brain;

    @Nullable
    private final BossController boss;

    private Location lastLocation;

    CustomMobInstance(CustomMobType<?> type, Mob entity, ModelInstance rig, @Nullable BossController boss,
                       KrimsonGoalSelector brain) {
        this.type = type;
        this.entity = entity;
        this.rig = rig;
        this.animation = new MobAnimationController(type, rig);
        this.boss = boss;
        this.brain = brain;
        this.lastLocation = entity.getLocation();
    }

    public CustomMobType<?> type() {
        return type;
    }

    public Mob entity() {
        return entity;
    }

    public ModelInstance rig() {
        return rig;
    }

    public MobAnimationController animation() {
        return animation;
    }

    @Nullable
    public BossController boss() {
        return boss;
    }

    /** Called by a custom {@code Goal} (or {@code CustomMobListener}) when the mob attacks. */
    public void triggerAttack() {
        animation.triggerAttack();
    }

    /** Called by {@code CustomMobListener} on damage. */
    public void triggerHurt() {
        animation.triggerHurt();
    }

    /** Called by {@code CustomMobListener} on death. */
    public void triggerDeath() {
        animation.triggerDeath();
    }

    public void triggerCustomAnimation(String animationName, float seconds) {
        animation.triggerCustom(animationName, seconds);
    }

    /** Advances the rig to follow the entity and ticks its animation state. Called by {@link CustomMobManager}. */
    void tick(float deltaSeconds) {
        // Always apply to be sure it's invisible
        entity.setInvisible(true);

        brain.tick(entity, deltaSeconds);

        Location current = entity.getLocation();
        double moved = current.distanceSquared(lastLocation);
        boolean moving = moved > 1.0E-5 || entity.getVelocity().lengthSquared() > 4.0E-3;

        rig.setYaw(entity.getBodyYaw());
        if (moved > 1.0E-6) {
            rig.teleport(current);
        }
        lastLocation = current;
        rig.tick(deltaSeconds);
        animation.tick(deltaSeconds, moving);

        if (boss != null) {
            boss.tick();
        }
    }

    /** Removes the rig (and boss bar, if any). Does not remove the backing entity. */
    void disposeVisuals() {
        brain.stopAll(entity);
        rig.remove();
        if (boss != null) {
            boss.dispose();
        }
    }
}
