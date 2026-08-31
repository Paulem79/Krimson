package net.paulem.krimson.mobs.ai.goals;

import net.paulem.krimson.mobs.ai.GoalCategory;
import net.paulem.krimson.mobs.ai.KrimsonGoal;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

/**
 * Bukkit-only replacement for vanilla's {@code FloatGoal}: nudges the mob upward while its
 * eyes are underwater so it doesn't sink and drown.
 */
public final class FloatGoal implements KrimsonGoal {
    private final int priority;

    public FloatGoal(int priority) {
        this.priority = priority;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public GoalCategory category() {
        return GoalCategory.MISC;
    }

    @Override
    public boolean canUse(Mob mob) {
        return mob.getEyeLocation().getBlock().getType() == Material.WATER;
    }

    @Override
    public void tick(Mob mob, float deltaSeconds) {
        Vector velocity = mob.getVelocity();
        if (velocity.getY() < 0.2) {
            mob.setVelocity(velocity.setY(0.2));
        }
    }
}
