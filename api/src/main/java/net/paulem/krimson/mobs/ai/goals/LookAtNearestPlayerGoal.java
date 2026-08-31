package net.paulem.krimson.mobs.ai.goals;

import net.paulem.krimson.mobs.ai.GoalCategory;
import net.paulem.krimson.mobs.ai.KrimsonGoal;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/** Bukkit-only replacement for vanilla's {@code LookAtPlayerGoal}. */
public final class LookAtNearestPlayerGoal implements KrimsonGoal {
    private final int priority;
    private final double range;

    public LookAtNearestPlayerGoal(int priority, double range) {
        this.priority = priority;
        this.range = range;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public GoalCategory category() {
        return GoalCategory.LOOK;
    }

    @Override
    public boolean canUse(Mob mob) {
        return nearestPlayer(mob) != null;
    }

    @Override
    public void tick(Mob mob, float deltaSeconds) {
        Player player = nearestPlayer(mob);
        if (player != null) {
            mob.lookAt(player, 10.0F, 40.0F);
        }
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
                && player.getGameMode() != org.bukkit.GameMode.SPECTATOR
                && player.getGameMode() != org.bukkit.GameMode.CREATIVE;
    }
}
