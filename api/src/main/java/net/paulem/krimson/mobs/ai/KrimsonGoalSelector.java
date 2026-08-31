package net.paulem.krimson.mobs.ai;

import org.bukkit.entity.Mob;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Drop-in replacement for vanilla's {@code GoalSelector}, minus NMS: picks and ticks at
 * most one {@link KrimsonGoal} per {@link GoalCategory} every server tick. Owned per-mob by
 * {@code CustomMobInstance}, ticked from {@code CustomMobManager}'s existing tick loop —
 * nothing here hooks the entity's own tick method.
 */
public final class KrimsonGoalSelector {
    private final List<KrimsonGoal> goals;
    private final Map<GoalCategory, KrimsonGoal> running = new EnumMap<>(GoalCategory.class);

    public KrimsonGoalSelector(List<KrimsonGoal> goals) {
        this.goals = goals.stream()
                .sorted(Comparator.comparingInt(KrimsonGoal::priority))
                .toList();
    }

    public void tick(Mob mob, float deltaSeconds) {
        for (GoalCategory category : GoalCategory.values()) {
            tickCategory(mob, deltaSeconds, category);
        }
    }

    private void tickCategory(Mob mob, float deltaSeconds, GoalCategory category) {
        KrimsonGoal current = running.get(category);

        if (current != null && !current.canContinueToUse(mob)) {
            current.stop(mob);
            running.remove(category);
            current = null;
        }

        if (current == null) {
            for (KrimsonGoal candidate : goals) {
                if (candidate.category() != category) {
                    continue;
                }
                if (candidate.canUse(mob)) {
                    candidate.start(mob);
                    running.put(category, candidate);
                    current = candidate;
                    break;
                }
            }
        }

        if (current != null) {
            current.tick(mob, deltaSeconds);
        }
    }

    /** Stops every currently running goal — call when the mob is removed. */
    public void stopAll(Mob mob) {
        for (KrimsonGoal goal : running.values()) {
            goal.stop(mob);
        }
        running.clear();
    }
}
