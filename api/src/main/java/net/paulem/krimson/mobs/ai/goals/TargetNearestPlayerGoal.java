package net.paulem.krimson.mobs.ai.goals;

import net.paulem.krimson.mobs.ai.GoalCategory;
import net.paulem.krimson.mobs.ai.KrimsonGoal;
import org.bukkit.GameMode;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/** Bukkit-only replacement for vanilla's {@code NearestAttackableTargetGoal<Player>}. */
public final class TargetNearestPlayerGoal implements KrimsonGoal {
    private final int priority;
    private final double range;
    private int rescanTicks;

    public TargetNearestPlayerGoal(int priority, double range) {
        this.priority = priority;
        this.range = range;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public GoalCategory category() {
        return GoalCategory.TARGET;
    }

    @Override
    public boolean canUse(Mob mob) {
        return mob.getTarget() == null && nearestPlayer(mob) != null;
    }

    @Override
    public boolean canContinueToUse(Mob mob) {
        Player target = (Player) mob.getTarget();
        return target != null && isTargetable(target)
                && target.getLocation().distanceSquared(mob.getLocation()) <= range * range;
    }

    @Override
    public void start(Mob mob) {
        mob.setTarget(nearestPlayer(mob));
    }

    @Override
    public void tick(Mob mob, float deltaSeconds) {
        if (--rescanTicks > 0) {
            return;
        }
        rescanTicks = 10;
        if (mob.getTarget() == null) {
            mob.setTarget(nearestPlayer(mob));
        }
    }

    @Override
    public void stop(Mob mob) {
        mob.setTarget(null);
    }

    private Player nearestPlayer(Mob mob) {
        Player nearest = null;
        double nearestDistance = range * range;
        for (Player player : mob.getWorld().getPlayers()) {
            if (!isTargetable(player)) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(mob.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static boolean isTargetable(Player player) {
        return player.isValid() && !player.isDead()
                && player.getGameMode() != GameMode.SPECTATOR
                && player.getGameMode() != GameMode.CREATIVE;
    }
}
