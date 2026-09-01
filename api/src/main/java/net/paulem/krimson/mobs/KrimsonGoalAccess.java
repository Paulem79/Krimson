package net.paulem.krimson.mobs;

import net.minecraft.world.entity.Mob;

/**
 * <p>This is the only place in the mob system that touches an NMS field directly. It stays
 * safe across Minecraft versions. If a future version ever
 * renames {@code goalSelector}/{@code targetSelector}, only this file needs updating.
 */
public final class KrimsonGoalAccess {
    private KrimsonGoalAccess() {
    }

    /** Wipes whatever goals the vanilla constructor registered on {@code mob}. */
    public static void clearVanillaGoals(Mob mob) {
        mob.goalSelector.removeAllGoals(goal -> true);
        mob.targetSelector.removeAllGoals(goal -> true);
    }
}
