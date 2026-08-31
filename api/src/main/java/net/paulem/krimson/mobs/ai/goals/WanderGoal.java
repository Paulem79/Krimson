package net.paulem.krimson.mobs.ai.goals;

import net.paulem.krimson.mobs.ai.GoalCategory;
import net.paulem.krimson.mobs.ai.KrimsonGoal;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import java.util.random.RandomGenerator;

/**
 * Bukkit-only replacement for vanilla's {@code WaterAvoidingRandomStrollGoal}: picks a
 * random nearby point and walks there via {@link Mob#getPathfinder()}.
 */
public final class WanderGoal implements KrimsonGoal {
    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    private final int priority;
    private final double speed;
    private final double radius;
    private long cooldownUntilTick;
    private int tick;

    public WanderGoal(int priority, double speed) {
        this(priority, speed, 10.0);
    }

    public WanderGoal(int priority, double speed, double radius) {
        this.priority = priority;
        this.speed = speed;
        this.radius = radius;
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
        return mob.getTarget() == null && tick >= cooldownUntilTick;
    }

    @Override
    public boolean canContinueToUse(Mob mob) {
        return mob.getTarget() == null && !mob.getPathfinder().hasPath();
    }

    @Override
    public void start(Mob mob) {
        Location origin = mob.getLocation();
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double distance = RANDOM.nextDouble() * radius;
        Vector offset = new Vector(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        Location target = origin.clone().add(offset);
        target.setY(mob.getWorld().getHighestBlockYAt(target) + 1);
        mob.getPathfinder().moveTo(target, speed);
    }

    @Override
    public void tick(Mob mob, float deltaSeconds) {
        tick++;
        if (!mob.getPathfinder().hasPath()) {
            cooldownUntilTick = tick + 40 + RANDOM.nextInt(60);
        }
    }

    @Override
    public void stop(Mob mob) {
        mob.getPathfinder().stopPathfinding();
    }
}
