package net.paulem.krimson.mobs.ai.goals;

import net.paulem.krimson.mobs.CustomMobInstance;
import net.paulem.krimson.mobs.CustomMobs;
import net.paulem.krimson.mobs.ai.GoalCategory;
import net.paulem.krimson.mobs.ai.KrimsonGoal;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * Bukkit-only replacement for vanilla's {@code MeleeAttackGoal}: paths to the mob's current
 * target and deals damage on contact, on a cooldown.
 */
public final class MeleeAttackGoal implements KrimsonGoal {
    private final int priority;
    private final double speed;
    private final double attackRangeSquared;
    private final int attackCooldownTicks;

    private int ticksUntilNextAttack;
    private int repathTicks;

    public MeleeAttackGoal(int priority, double speed) {
        this(priority, speed, 2.0, 20);
    }

    public MeleeAttackGoal(int priority, double speed, double attackRange, int attackCooldownTicks) {
        this.priority = priority;
        this.speed = speed;
        this.attackRangeSquared = attackRange * attackRange;
        this.attackCooldownTicks = attackCooldownTicks;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public GoalCategory category() {
        return GoalCategory.MOVEMENT;
    }

    @Override
    public boolean canUse(Mob mob) {
        LivingEntity target = mob.getTarget();
        return target != null && target.isValid() && !target.isDead();
    }

    @Override
    public boolean canContinueToUse(Mob mob) {
        return canUse(mob);
    }

    @Override
    public void start(Mob mob) {
        ticksUntilNextAttack = 0;
        repathTicks = 0;
    }

    @Override
    public void tick(Mob mob, float deltaSeconds) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }

        if (--repathTicks <= 0) {
            mob.getPathfinder().moveTo(target, speed);
            repathTicks = 10;
        }

        if (ticksUntilNextAttack > 0) {
            ticksUntilNextAttack--;
            return;
        }

        double distanceSquared = mob.getEyeLocation().distanceSquared(target.getEyeLocation());
        if (distanceSquared <= attackRangeSquared) {
            double damage = attackDamage(mob);
            target.damage(damage, mob);
            triggerAttackAnimation(mob);
            ticksUntilNextAttack = attackCooldownTicks;
        }
    }

    @Override
    public void stop(Mob mob) {
        mob.getPathfinder().stopPathfinding();
    }

    private static double attackDamage(Mob mob) {
        var attribute = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        return attribute != null ? attribute.getValue() : 1.0;
    }

    private static void triggerAttackAnimation(Mob mob) {
        CustomMobInstance instance = CustomMobs.manager().instanceOf(mob);
        if (instance != null) {
            instance.triggerAttack();
        }
    }
}
